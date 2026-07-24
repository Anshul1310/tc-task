const axios = require("axios");
const prisma = require("../config/db");
const { findSimilarDiscussions } = require("../services/embeddingService");

async function textSearch(req, res) {
  try {
    const { query } = req.body;

    if (!query) {
      return res.status(400).json({ error: "Query text is required" });
    }

    const matches = await findSimilarDiscussions(query, null, null, 10);

    res.json({ matches });
  } catch (error) {
    console.error("Text search error:", error.message);
    res.status(500).json({ error: "Text search failed" });
  }
}

async function imageSearch(req, res) {
  try {
    if (!req.file) {
      return res.status(400).json({ error: "Image file is required" });
    }

    const imagePath = `uploads/${req.file.filename}`;
    const matches = await findSimilarDiscussions(null, null, imagePath, 10);

    res.json({ matches });
  } catch (error) {
    console.error("Image search error:", error.message);
    res.status(500).json({ error: "Image search failed" });
  }
}

/**
 * RAG Search using Groq AI (LLaMA 3.3 70B):
 * Generates a concise (~80-120 words) mentor summary with HIGH WEIGHTAGE
 * on retrieved post titles, descriptions, and user comments.
 */
async function ragSearch(req, res) {
  try {
    const query = req.body.query || req.query.query;

    if (!query || typeof query !== "string" || !query.trim()) {
      return res.status(400).json({ error: "Search query is required" });
    }

    const SIMILARITY_THRESHOLD = 0.55;

    // 1. Vector Search in ChromaDB
    const vectorMatches = await findSimilarDiscussions(query.trim(), null, null, 10);

    let topMatches = vectorMatches.filter((m) => m.similarity >= SIMILARITY_THRESHOLD);
    if (topMatches.length === 0 && vectorMatches.length > 0) {
      topMatches = vectorMatches.slice(0, 3);
    }

    const discussionIds = topMatches.map((m) => m.discussionId);

    let discussions = [];
    if (discussionIds.length > 0) {
      // 2. Fetch full matching discussions & comments from PostgreSQL
      const dbDiscussions = await prisma.discussion.findMany({
        where: { id: { in: discussionIds } },
        include: {
          createdBy: { select: { id: true, anonymousUsername: true, avatarColor: true } },
          comments: {
            include: {
              author: { select: { id: true, anonymousUsername: true, avatarColor: true } },
              replies: {
                include: { author: { select: { id: true, anonymousUsername: true, avatarColor: true } } }
              }
            }
          },
          _count: { select: { comments: true } }
        }
      });

      // Preserve vector similarity order
      const similarityMap = new Map(topMatches.map((m) => [m.discussionId, m.similarity]));
      discussions = dbDiscussions
        .map((d) => ({
          ...d,
          similarity: similarityMap.get(d.id) || 0
        }))
        .sort((a, b) => b.similarity - a.similarity);
    }

    // 3. Build RAG Context String
    let contextText = "";
    if (discussions.length > 0) {
      discussions.forEach((d, idx) => {
        contextText += `\n[Campus Discussion #${idx + 1}]\n`;
        contextText += `Title: ${d.title}\n`;
        if (d.buildingName) contextText += `Location/Department: ${d.buildingName}\n`;
        contextText += `Post Description: ${d.description}\n`;
        if (d.comments && d.comments.length > 0) {
          contextText += `Student Comments & Discussion Updates:\n`;
          d.comments.forEach((c) => {
            contextText += `  - Comment by ${c.author?.anonymousUsername || "Student"}: "${c.text}"\n`;
            if (c.replies) {
              c.replies.forEach((r) => {
                contextText += `    * Reply by ${r.author?.anonymousUsername || "Student"}: "${r.text}"\n`;
              });
            }
          });
        } else {
          contextText += `Comments: No comments posted yet.\n`;
        }
      });
    }

    // 4. Generate Concise, High-Weightage Mentor Advice using Groq API
    const groqApiKey = process.env.GROQ_API_KEY;
    let aiAnswer = "";

    if (groqApiKey) {
      const systemPrompt = `You are a concise, insightful, and encouraging Senior Academic & Campus Mentor at NIT Trichy. Your job is to analyze campus community posts, descriptions, and comments, and deliver a crisp, high-weightage summary for the student.`;

      const userPrompt = `Student Query: "${query}"

RETRIEVED CAMPUS COMMUNITY DATA (Post Titles, Descriptions, Locations & User Comments):
${contextText.length > 0 ? contextText : "No specific matching posts retrieved."}

MANDATORY INSTRUCTIONS:
1. RESPONSE LENGTH: Keep your response CRISP and CONCISE (around 80 to 120 words total). Perfect for mobile reading!
2. HIGH WEIGHTAGE ON POSTS & COMMENTS: Give HIGHEST PRIORITY to analyzing the specific post titles, descriptions, and student comments retrieved above. Directly cite and address what users reported and commented (including any status updates or fixes).
3. MENTOR ADVICE & CONCLUSION: Structure your answer warmly. Summarize the post & comment details clearly, and conclude with a direct recommendation (e.g., "So based on student feedback and comments, you can proceed with this choice because...").`;

      const groqModelsToTry = [
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768"
      ];

      for (const modelName of groqModelsToTry) {
        try {
          const groqRes = await axios.post(
            "https://api.groq.com/openai/v1/chat/completions",
            {
              model: modelName,
              messages: [
                { role: "system", content: systemPrompt },
                { role: "user", content: userPrompt }
              ],
              temperature: 0.6,
              max_tokens: 500
            },
            {
              headers: {
                "Authorization": `Bearer ${groqApiKey}`,
                "Content-Type": "application/json"
              }
            }
          );

          const responseText = groqRes.data?.choices?.[0]?.message?.content;
          if (responseText && responseText.trim().length > 30) {
            aiAnswer = responseText.trim();
            break; // Success!
          }
        } catch (err) {
          const errMsg = err.response?.data?.error?.message || err.message;
          console.error(`Groq model ${modelName} call failed:`, errMsg);
        }
      }
    }

    // Fallback Teacher synthesis if Groq API key is missing or calls fail
    if (!aiAnswer && discussions.length > 0) {
      const topMatchesSummary = discussions.map(d => {
        let summary = `• "${d.title}" (${d.buildingName || 'Campus'}): ${d.description}`;
        if (d.comments && d.comments.length > 0) {
          summary += ` [Comment: "${d.comments[0].text}"]`;
        }
        return summary;
      }).join('\n');

      aiAnswer = `Hello! Based on student posts and comments regarding "${query}":\n\n${topMatchesSummary}\n\nBased on community feedback, you can proceed with confidence!`;
    }

    res.json({
      answer: aiAnswer,
      matches: discussions
    });
  } catch (error) {
    console.error("RAG search error:", error.message);
    res.status(500).json({ error: "RAG search failed" });
  }
}

module.exports = { textSearch, imageSearch, ragSearch };
