// ──────────────────────────────────────────────
// Embedding Service (Local Inference)
//
// Generates text and image embeddings using
// local models via Xenova/transformers.js.
// No external API calls are made.
//
// Models used:
//   Text:  Xenova/all-MiniLM-L6-v2  → 384-dim vectors
//   Image: Xenova/clip-vit-base-patch32 → 512-dim vectors
// ──────────────────────────────────────────────

const fs = require("fs");
const path = require("path");

const TEXT_MODEL = "Xenova/all-MiniLM-L6-v2";
const IMAGE_MODEL = "Xenova/clip-vit-base-patch32";

// Singletons for pipelines
let textPipelinePromise = null;
let imagePipelinePromise = null;

// Dynamically import transformers to avoid top-level await issues
// in CommonJS modules if necessary, but requires() works natively in node
async function getPipeline() {
  const { pipeline, env } = await import('@xenova/transformers');
  // Optional: Set cache directory if needed
  // env.cacheDir = path.join(__dirname, '../../.cache');
  return pipeline;
}

async function getTextExtractor() {
  if (!textPipelinePromise) {
    console.log(`[Embeddings] Loading local text model: ${TEXT_MODEL}`);
    textPipelinePromise = getPipeline().then(pipeline => 
      pipeline("feature-extraction", TEXT_MODEL)
    );
  }
  return textPipelinePromise;
}

async function getImageExtractor() {
  if (!imagePipelinePromise) {
    console.log(`[Embeddings] Loading local image model: ${IMAGE_MODEL}`);
    imagePipelinePromise = getPipeline().then(pipeline => 
      pipeline("image-feature-extraction", IMAGE_MODEL)
    );
  }
  return imagePipelinePromise;
}

// ── Text Embedding ────────────────────────────
// Generates a 384-dimensional float array natively.
async function getTextEmbedding(text) {
  try {
    const extractor = await getTextExtractor();
    const output = await extractor(text, { pooling: "mean", normalize: true });
    // output.data is a Float32Array
    return Array.from(output.data);
  } catch (error) {
    console.error("Text embedding error:", error.message);
    throw new Error("Failed to generate local text embedding");
  }
}

// ── Image Embedding ───────────────────────────
// Generates a 512-dimensional float array natively.
async function getImageEmbedding(imagePath) {
  try {
    const extractor = await getImageExtractor();
    
    // Convert relative/absolute paths to proper file:// URLs if needed,
    // though Transformers.js usually handles absolute paths fine.
    const output = await extractor(imagePath);
    return Array.from(output.data);
  } catch (error) {
    console.error("Image embedding error:", error.message);
    throw new Error("Failed to generate local image embedding");
  }
}

module.exports = {
  getTextEmbedding,
  getImageEmbedding,
};
