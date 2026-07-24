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
 * RAG Search: Filters top matching posts with similarity >= 0.85 (85%),
 * compiles their titles, descriptions, and comment sections,
 * and uses Google Gemini to summarize them as an AI Answer for the user query.
 */
async function ragSearch(req, res) {
  try {
    const query = req.body.query || req.query.query;

    if (!query || typeof query !== "string" || !query.trim()) {
      return res.status(400).json({ error: "Search query is required" });
    }

    const SIMILARITY_THRESHOLD = 0.85;

    // 1. Vector Search in ChromaDB
    const vectorMatches = await findSimilarDiscussions(query.trim(), null, null, 10);

    // Filter only top matching posts with similarity >= 85%
    const topMatches = vectorMatches.filter((m) => m.similarity >= SIMILARITY_THRESHOLD);
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

    // 3. Build RAG Context (Posts + Descriptions + Comments)
    let contextText = "";
    if (discussions.length > 0) {
      discussions.forEach((d, idx) => {
        contextText += `\n[Matching Post #${idx + 1} - Similarity Match: ${(d.similarity * 100).toFixed(0)}%]\n`;
        contextText += `Title: ${d.title}\n`;
        if (d.buildingName) contextText += `Location: ${d.buildingName}\n`;
        contextText += `Description: ${d.description}\n`;
        if (d.comments && d.comments.length > 0) {
          contextText += `Comments & Discussion Updates:\n`;
          d.comments.forEach((c) => {
            contextText += `  - Comment by ${c.author?.anonymousUsername || "User"}: "${c.text}"\n`;
            if (c.replies) {
              c.replies.forEach((r) => {
                contextText += `    * Reply by ${r.author?.anonymousUsername || "User"}: "${r.text}"\n`;
              });
            }
          });
        } else {
          contextText += `Comments: No comments yet.\n`;
        }
      });
    }

    // 4. Generate AI Summary using Google Gemini API
    const apiKey = process.env.GEMINI_API_KEY;
    let aiAnswer = "";

    if (discussions.length === 0) {
      aiAnswer = `No campus discussions or comments with high relevance (≥ 85% similarity) were found matching "${query}". Feel free to create a new discussion topic!`;
    } else if (apiKey && contextText.length > 0) {
      const prompt = `You are CampusCare AI, a campus assistant for students at NIT Trichy.
The student asked: "${query}"

Here are the top relevant campus posts, their descriptions, and user comments (filtered for >= 85% similarity):
${contextText}

Instructions:
1. Summarize the top matching posts, their descriptions, and user comments clearly as an AI Summary.
2. Directly answer the user's question based on the post descriptions and comments.
3. If comments mention an update, resolution, or solution (e.g. fixed, found, resolved), highlight it in your summary.
4. Keep the summary structured, helpful, and concise.`;

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

    // Fallback AI synthesis if Gemini API key is missing or call fails
    if (!aiAnswer && discussions.length > 0) {
      const topMatchesSummary = discussions.map(d => {
        let summary = `• "${d.title}" (${d.buildingName || 'Campus'}): ${d.description}`;
        if (d.comments && d.comments.length > 0) {
          summary += ` [Top Comment: "${d.comments[0].text}"]`;
        }
        return summary;
      }).join('\n\n');

      aiAnswer = `Here is an AI summary of top matching campus posts (≥ 85% similarity) for "${query}":\n\n${topMatchesSummary}`;
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
