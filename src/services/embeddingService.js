// ──────────────────────────────────────────────
// Embedding & Vector Service Client
//
// Communicates with the Python sidecar server over HTTP.
// The Python sidecar manages FastEmbed (ONNX) and
// ChromaDB (persistent vector storage in python-embeddings/chroma_db/).
// ──────────────────────────────────────────────

const axios = require("axios");
const fs = require("fs");
const path = require("path");
const FormData = require("form-data");

const EMBEDDING_URL = process.env.EMBEDDING_SERVICE_URL || "http://localhost:5050";

// Helper to resolve image paths stored in database/uploads
function getAbsolutePath(imagePath) {
  if (!imagePath) return null;
  return path.join(__dirname, "../", imagePath);
}

// ── Index a Discussion in ChromaDB ────────────
async function indexDiscussion(discussionId, title, description, imagePath) {
  try {
    const form = new FormData();
    form.append("discussion_id", String(discussionId));
    form.append("title", title);
    form.append("description", description);

    if (imagePath) {
      const absolutePath = getAbsolutePath(imagePath);
      if (fs.existsSync(absolutePath)) {
        form.append("file", fs.createReadStream(absolutePath));
      }
    }

    const response = await axios.post(`${EMBEDDING_URL}/discussions/index`, form, {
      headers: form.getHeaders(),
    });

    return response.data;
  } catch (error) {
    console.error("Index discussion error:", error.message);
    // Non-blocking: fail gracefully if Python server fails
    return null;
  }
}

// ── Find Similar Discussions via ChromaDB ────
async function findSimilarDiscussions(title, description, imagePath, limit = 5) {
  try {
    const form = new FormData();
    if (title) form.append("title", title);
    if (description) form.append("description", description);
    form.append("limit", String(limit));

    if (imagePath) {
      const absolutePath = getAbsolutePath(imagePath);
      if (fs.existsSync(absolutePath)) {
        form.append("file", fs.createReadStream(absolutePath));
      }
    }

    const response = await axios.post(`${EMBEDDING_URL}/discussions/find_similar`, form, {
      headers: form.getHeaders(),
    });

    return response.data.matches || [];
  } catch (error) {
    console.error("Find similar discussions error:", error.message);
    return [];
  }
}

// ── Delete Vector from ChromaDB ───────────────
async function deleteDiscussionVector(discussionId) {
  try {
    const response = await axios.delete(`${EMBEDDING_URL}/discussions/${discussionId}`);
    return response.data;
  } catch (error) {
    console.error("Delete discussion vector error:", error.message);
    return null;
  }
}

module.exports = {
  indexDiscussion,
  findSimilarDiscussions,
  deleteDiscussionVector,
};
