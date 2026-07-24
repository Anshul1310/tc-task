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
 * RAG Search: Retrieves top matching discussions + comments via ChromaDB
 * and generates a direct AI answer using Google Gemini.
 */
async function ragSearch(req, res) {
  try {
    const query = req.body.query || req.query.query;

    if (!query || typeof query !== "string" || !query.trim()) {
      return res.status(400).json({ error: "Search query is required" });
    }

    // 1. Vector Search in ChromaDB
    const vectorMatches = await findSimilarDiscussions(query.trim(), null, null, 5);
    const discussionIds = vectorMatches.map((m) => m.discussionId);

    let discussions = [];
    if (discussionIds.length > 0) {
      // 2. Fetch full discussions & comments from PostgreSQL
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
      const similarityMap = new Map(vectorMatches.map((m) => [m.discussionId, m.similarity]));
      discussions = dbDiscussions
        .map((d) => ({
          ...d,
          similarity: similarityMap.get(d.id) || 0
        }))
        .sort((a, b) => b.similarity - a.similarity);
    }

    // 3. Build RAG Context String (Posts + Comments + Replies)
    let contextText = "";
    if (discussions.length > 0) {
      discussions.forEach((d, idx) => {
        contextText += `\n[Discussion #${idx + 1}]\n`;
        contextText += `Title: ${d.title}\n`;
        if (d.buildingName) contextText += `Location: ${d.buildingName}\n`;
        contextText += `Post Body: ${d.description}\n`;
        if (d.comments && d.comments.length > 0) {
          contextText += `Comments & Updates:\n`;
          d.comments.forEach((c) => {
            contextText += `  - ${c.author?.anonymousUsername || "User"}: "${c.text}"\n`;
            if (c.replies) {
              c.replies.forEach((r) => {
                contextText += `    * Reply by ${r.author?.anonymousUsername || "User"}: "${r.text}"\n`;
              });
            }
          });
        }
      });
    }

    // 4. Generate AI Answer using Google Gemini API
    const apiKey = process.env.GEMINI_API_KEY;
    let aiAnswer = "";

    if (apiKey && contextText.length > 0) {
      const prompt = `You are CampusCare AI, a campus assistant for students at NIT Trichy.
Answer the student's question clearly, concisely, and accurately based ONLY on the retrieved campus discussions and comments below.
If comments mention an update or resolution (e.g. maintenance fixed a leak, item was found, etc.), include that in your answer!

Student Question: "${query}"

Retrieved Campus Posts & Comments:
${contextText}`;

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
    if (!aiAnswer) {
      if (discussions.length > 0) {
        const topMatchesSummary = discussions.slice(0, 3).map(d => {
          let summary = `• "${d.title}" (${d.buildingName || 'Campus'})`;
          if (d.comments && d.comments.length > 0) {
            summary += ` — Latest comment: "${d.comments[0].text}"`;
          }
          return summary;
        }).join('\n');

        aiAnswer = `Based on campus discussions for "${query}":\n\n${topMatchesSummary}`;
      } else {
        aiAnswer = `No campus discussions or comments were found matching "${query}". You can create a new post to ask the community!`;
      }
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
