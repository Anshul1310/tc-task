// ──────────────────────────────────────────────
// Embedding Service
//
// Generates text and image embeddings using
// Hugging Face's Hosted Inference API.
//
// DESIGN: This file is the ONLY place that talks
// to Hugging Face. When you want to switch to
// local models later, you only change this file.
// Controllers and routes stay the same.
//
// Models used:
//   Text:  BAAI/bge-small-en-v1.5  → 384-dim vectors
//   Image: openai/clip-vit-base-patch32 → 512-dim vectors
// ──────────────────────────────────────────────

const axios = require("axios");
const fs = require("fs");
const path = require("path");
const prisma = require("../config/db");

const HF_API_URL = "https://api-inference.huggingface.co/models";
const TEXT_MODEL = "BAAI/bge-small-en-v1.5";
const IMAGE_MODEL = "openai/clip-vit-base-patch32";

function getHeaders() {
  return {
    Authorization: `Bearer ${process.env.HUGGINGFACE_API_KEY}`,
  };
}

// ── Text Embedding ────────────────────────────
// Sends a string to BGE-small and gets back a
// 384-dimensional float array.
async function getTextEmbedding(text) {
  try {
    const response = await axios.post(
      `${HF_API_URL}/${TEXT_MODEL}`,
      { inputs: text },
      { headers: getHeaders() }
    );

    // HF returns either a flat array or nested array
    const embedding = Array.isArray(response.data[0])
      ? response.data[0]
      : response.data;

    return embedding;
  } catch (error) {
    console.error("Text embedding error:", error.message);
    throw new Error("Failed to generate text embedding");
  }
}

// ── Image Embedding ───────────────────────────
// Reads an image file, sends it as binary to CLIP,
// and gets back a 512-dimensional float array.
async function getImageEmbedding(imagePath) {
  try {
    // Resolve path relative to project root
    const fullPath = path.join(__dirname, "..", imagePath);
    const imageBuffer = fs.readFileSync(fullPath);

    const response = await axios.post(
      `${HF_API_URL}/${IMAGE_MODEL}`,
      imageBuffer,
      {
        headers: {
          ...getHeaders(),
          "Content-Type": "application/octet-stream",
        },
      }
    );

    // CLIP returns the embedding as a flat array
    const embedding = Array.isArray(response.data[0])
      ? response.data[0]
      : response.data;

    return embedding;
  } catch (error) {
    console.error("Image embedding error:", error.message);
    throw new Error("Failed to generate image embedding");
  }
}

// ── Generate & Store ──────────────────────────
// Called after an item is created. Generates both
// embeddings and stores them in the DB using raw
// SQL (because Prisma can't handle vector columns).
async function generateAndStoreEmbeddings(itemId, title, description, images) {
  // Combine title + description for a richer text embedding
  const textInput = `${title}. ${description}`;
  const textEmbedding = await getTextEmbedding(textInput);

  // Use the first image for the image embedding (if any)
  let imageEmbedding = null;
  if (images && images.length > 0) {
    try {
      imageEmbedding = await getImageEmbedding(images[0]);
    } catch (err) {
      console.error("Skipping image embedding:", err.message);
    }
  }

  // Store embeddings using raw SQL
  // Prisma can't write to vector columns, so we use $executeRawUnsafe
  const textVector = `[${textEmbedding.join(",")}]`;

  if (imageEmbedding) {
    const imageVector = `[${imageEmbedding.join(",")}]`;
    await prisma.$executeRawUnsafe(
      `UPDATE "Item" SET "textEmbedding" = $1::vector, "imageEmbedding" = $2::vector WHERE id = $3`,
      textVector,
      imageVector,
      itemId
    );
  } else {
    await prisma.$executeRawUnsafe(
      `UPDATE "Item" SET "textEmbedding" = $1::vector WHERE id = $2`,
      textVector,
      itemId
    );
  }
}

module.exports = {
  getTextEmbedding,
  getImageEmbedding,
  generateAndStoreEmbeddings,
};
