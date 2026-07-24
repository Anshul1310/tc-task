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
 * RAG Search using Groq AI (LLaMA 3.3 70B / 3.1 8B):
 * Combines Groq LLM's vast domain & academic knowledge with retrieved campus posts,
 * titles, descriptions, and comments to generate rich, multi-paragraph mentor advice.
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
          contextText += `Student Comments & Feedback:\n`;
          d.comments.forEach((c) => {
            contextText += `  - ${c.author?.anonymousUsername || "Student"}: "${c.text}"\n`;
            if (c.replies) {
              c.replies.forEach((r) => {
                contextText += `    * Reply by ${r.author?.anonymousUsername || "Student"}: "${r.text}"\n`;
              });
            }
          });
        }
      });
    }

    // 4. Generate Detailed Mentor Advice using Groq API (LLaMA 3.3 70B)
    const groqApiKey = process.env.GROQ_API_KEY;
    let aiAnswer = "";

    if (groqApiKey) {
      const systemPrompt = `You are a highly experienced, articulate, and encouraging Senior Academic & Campus Mentor at NIT Trichy.`;

      const userPrompt = `A student came to you asking for advice on: "${query}"

Here is the campus community data (post titles, descriptions, locations, and student comments) retrieved from our platform:
${contextText.length > 0 ? contextText : "No specific posts retrieved, use your domain knowledge."}

MANDATORY RESPONSE LENGTH & FORMAT RULES:
1. YOU MUST GENERATE A COMPREHENSIVE, MULTI-PARAGRAPH MENTOR RESPONSE OF AT LEAST 150 TO 250 WORDS. DO NOT RETURN SHORT ANSWERS.
2. Structure your response into clear sections:
   • 🎓 Mentor's Overview & Greeting: Warmly greet the student and introduce your analysis.
   • 📊 Detailed Campus Analysis: Explain the post titles, descriptions, and student comments in depth, highlighting key trends, positives, and concerns.
   • 💡 Strategic Advisory & Recommendation: Give your explicit mentor recommendation (e.g., "So you can comfortably opt for this branch/choice because... but still keep in mind...").
3. Combine your OWN vast academic domain knowledge (about NIT Trichy, course curriculums, branch culture, career placements, and campus life) together with the student discussions above.
4. Speak in an inspiring, articulate, and supportive mentoring tone.`;

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
              temperature: 0.7,
              max_tokens: 2048
            },
            {
              headers: {
                "Authorization": `Bearer ${groqApiKey}`,
                "Content-Type": "application/json"
              }
            }
          );

          const responseText = groqRes.data?.choices?.[0]?.message?.content;
          if (responseText && responseText.trim().length > 50) {
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
          summary += `\n  - Student Comment: "${d.comments[0].text}"`;
        }
        return summary;
      }).join('\n\n');

      aiAnswer = `🎓 **Mentor's Overview & Greeting**\nHello! Based on our campus community discussions regarding "${query}", I've analyzed the available student posts and feedback for you.\n\n📊 **Detailed Campus Analysis**\n${topMatchesSummary}\n\n💡 **Strategic Advisory & Recommendation**\nYou can proceed confidently with this decision based on positive community feedback! Keep exploring and feel free to reach out to senior students for further insights.`;
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
