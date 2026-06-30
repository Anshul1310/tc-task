// ──────────────────────────────────────────────
// Search Controller
//
// Handles similarity search endpoints:
//   POST /search/text  → Find items similar to a text query
//   POST /search/image → Find items similar to an uploaded image
// ──────────────────────────────────────────────

const { getTextEmbedding, getImageEmbedding } = require("../services/embeddingService");
const { searchByVector } = require("../prisma/vectors");

// ── Text Search ───────────────────────────────
// User sends a text description, we generate an
// embedding and find visually/semantically similar items.
async function textSearch(req, res) {
  try {
    const { query } = req.body;

    if (!query) {
      return res.status(400).json({ error: "Query text is required" });
    }

    // Generate embedding for the search query
    const embedding = await getTextEmbedding(query);

    // Search against all items using cosine similarity
    const matches = await searchByVector(embedding, "text");

    res.json({ matches });
  } catch (error) {
    console.error("Text search error:", error.message);
    res.status(500).json({ error: "Text search failed" });
  }
}

// ── Image Search ──────────────────────────────
// User uploads an image, we generate a CLIP embedding
// and find visually similar items.
async function imageSearch(req, res) {
  try {
    if (!req.file) {
      return res.status(400).json({ error: "Image file is required" });
    }

    const imagePath = `uploads/${req.file.filename}`;

    // Generate CLIP embedding for the uploaded image
    const embedding = await getImageEmbedding(imagePath);

    // Search against all items using cosine similarity
    const matches = await searchByVector(embedding, "image");

    res.json({ matches });
  } catch (error) {
    console.error("Image search error:", error.message);
    res.status(500).json({ error: "Image search failed" });
  }
}

module.exports = { textSearch, imageSearch };
