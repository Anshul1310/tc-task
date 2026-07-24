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
 * RAG Search: Retrieves matching posts (with lowered similarity threshold >= 0.55),
 * and uses Google Gemini to explain the titles, descriptions, and comments
 * like a warm, supportive teacher/mentor guiding a student.
 */
async function ragSearch(req, res) {
  try {
    const query = req.body.query || req.query.query;

    if (!query || typeof query !== "string" || !query.trim()) {
      return res.status(400).json({ error: "Search query is required" });
    }

    // Lowered similarity threshold (55%) to capture broader relevant discussions
    const SIMILARITY_THRESHOLD = 0.55;

    // 1. Vector Search in ChromaDB
    const vectorMatches = await findSimilarDiscussions(query.trim(), null, null, 10);

    // Filter matching posts (threshold 55%, fallback to top 3 if none reach 55%)
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

    // 3. Build RAG Context (Post Titles + Descriptions + Comment Threads)
    let contextText = "";
    if (discussions.length > 0) {
      discussions.forEach((d, idx) => {
        contextText += `\n[Campus Discussion #${idx + 1} - Match Relevance: ${(d.similarity * 100).toFixed(0)}%]\n`;
        contextText += `Title: ${d.title}\n`;
        if (d.buildingName) contextText += `Location/Spot: ${d.buildingName}\n`;
        contextText += `Post Description: ${d.description}\n`;
        if (d.comments && d.comments.length > 0) {
          contextText += `Student Comments & Discussion Updates:\n`;
          d.comments.forEach((c) => {
            contextText += `  - ${c.author?.anonymousUsername || "Student"}: "${c.text}"\n`;
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

    // 4. Generate Teacher/Mentor Explanation using Google Gemini API
    const apiKey = process.env.GEMINI_API_KEY;
    let aiAnswer = "";

    if (discussions.length === 0) {
      aiAnswer = `Hello there! I reviewed our campus discussion records, but couldn't find any existing posts related to "${query}". Feel free to create a new discussion topic so fellow students and staff can assist you!`;
    } else if (apiKey && contextText.length > 0) {
      const prompt = `You are a warm, supportive, and wise campus Mentor & Teacher at NIT Trichy.
A student came to you asking for guidance on: "${query}"

Here is the campus community data (post titles, descriptions, locations, and student comments) retrieved from the platform:
${contextText}

Instructions for your Teacher/Mentor response:
1. Adopt a warm, encouraging, and clear teaching tone (e.g. "Hello! Let me break down what our campus community has reported...").
2. Answer the student's question thoroughly using the post titles, descriptions, and comments as your dataset.
3. Clearly explain what the posts describe and what the student comments reveal (including any recent updates, fixes, or solutions).
4. Provide actionable, supportive advice on what the student should do next based on this campus data.`;

      try {
        const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`;
        const geminiRes = await axios.post(geminiUrl, {
          contents: [{ parts: [{ text: prompt }] }]
        });

        aiAnswer = geminiRes.data?.candidates?.[0]?.content?.parts?.[0]?.text || "";
      } catch (geminiErr) {
        console.error("Gemini RAG API call error:", geminiErr.message);
      }
    }

    // Fallback Teacher synthesis if Gemini API key is missing or call fails
    if (!aiAnswer && discussions.length > 0) {
      const topMatchesSummary = discussions.map(d => {
        let summary = `• Post "${d.title}" (${d.buildingName || 'Campus'}): ${d.description}`;
        if (d.comments && d.comments.length > 0) {
          summary += `\n  - Latest Comment: "${d.comments[0].text}"`;
        }
        return summary;
      }).join('\n\n');

      aiAnswer = `Hello! Based on the campus community discussions regarding "${query}", here is what I found for you:\n\n${topMatchesSummary}\n\nHope this helps! Let me know if you need anything else.`;
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
