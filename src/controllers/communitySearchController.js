const { getTextEmbedding, getImageEmbedding } = require("../services/embeddingService");
const { findSimilarDiscussions } = require("../prisma/vectors");

async function textSearch(req, res) {
  try {
    const { query } = req.body;

    if (!query) {
      return res.status(400).json({ error: "Query text is required" });
    }

    const embedding = await getTextEmbedding(query);
    const matches = await findSimilarDiscussions(embedding, null, 10);

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
    const embedding = await getImageEmbedding(imagePath);
    const matches = await findSimilarDiscussions(null, embedding, 10);

    res.json({ matches });
  } catch (error) {
    console.error("Image search error:", error.message);
    res.status(500).json({ error: "Image search failed" });
  }
}

module.exports = { textSearch, imageSearch };
