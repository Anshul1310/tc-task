// ──────────────────────────────────────────────
// Item Controller
//
// CRUD operations for lost and found items.
// Every function follows the same pattern:
//   1. Read input from req (params, body, files)
//   2. Talk to the database via Prisma
//   3. Send a JSON response
// ──────────────────────────────────────────────

const prisma = require("../config/db");
const { generateAndStoreEmbeddings } = require("../services/embeddingService");
const { findSimilarItems } = require("../prisma/vectors");

// ── Create a new lost or found item ───────────
async function createItem(req, res) {
  try {
    const { title, description, category, location, dateLostOrFound, status } =
      req.body;

    // Collect file paths from Multer
    const images = req.files
      ? req.files.map((file) => `uploads/${file.filename}`)
      : [];

    const item = await prisma.item.create({
      data: {
        title,
        description,
        category,
        location,
        dateLostOrFound: new Date(dateLostOrFound),
        status: status || "LOST",
        images,
        userId: req.user.id,
      },
    });

    // Generate embeddings in the background (don't block the response)
    // If it fails, the item is still created — embeddings can be retried
    generateAndStoreEmbeddings(item.id, title, description, images).catch(
      (err) => console.error("Embedding generation failed:", err.message)
    );

    // Find similar items (opposite type: LOST ↔ FOUND)
    const searchAgainst = status === "FOUND" ? "LOST" : "FOUND";
    let matches = [];
    try {
      matches = await findSimilarItems(item.id, searchAgainst, "text");
    } catch {
      // Embeddings might not be stored yet — that's OK
    }

    res.status(201).json({ item, matches });
  } catch (error) {
    console.error("Create item error:", error.message);
    res.status(500).json({ error: "Failed to create item" });
  }
}

// ── Get all items (with optional status filter) ─
async function getAllItems(req, res) {
  try {
    const { status } = req.query;

    const where = {};
    if (status) where.status = status;

    const items = await prisma.item.findMany({
      where,
      include: { user: { select: { id: true, name: true, email: true } } },
      orderBy: { createdAt: "desc" },
    });

    res.json({ items });
  } catch (error) {
    console.error("Get items error:", error.message);
    res.status(500).json({ error: "Failed to fetch items" });
  }
}

// ── Get a single item by ID ──────────────────
async function getItemById(req, res) {
  try {
    const item = await prisma.item.findUnique({
      where: { id: parseInt(req.params.id) },
      include: { user: { select: { id: true, name: true, email: true } } },
    });

    if (!item) {
      return res.status(404).json({ error: "Item not found" });
    }

    res.json({ item });
  } catch (error) {
    console.error("Get item error:", error.message);
    res.status(500).json({ error: "Failed to fetch item" });
  }
}

// ── Update own item ──────────────────────────
async function updateItem(req, res) {
  try {
    const itemId = parseInt(req.params.id);

    // Check ownership — users can only edit their own items
    const existing = await prisma.item.findUnique({ where: { id: itemId } });
    if (!existing) return res.status(404).json({ error: "Item not found" });
    if (existing.userId !== req.user.id) {
      return res.status(403).json({ error: "You can only edit your own items" });
    }

    const { title, description, category, location, dateLostOrFound } =
      req.body;

    // If new images were uploaded, add them to the existing ones
    const newImages = req.files
      ? req.files.map((file) => `uploads/${file.filename}`)
      : [];
    const images =
      newImages.length > 0
        ? [...existing.images, ...newImages]
        : existing.images;

    const item = await prisma.item.update({
      where: { id: itemId },
      data: {
        ...(title && { title }),
        ...(description && { description }),
        ...(category && { category }),
        ...(location && { location }),
        ...(dateLostOrFound && {
          dateLostOrFound: new Date(dateLostOrFound),
        }),
        images,
      },
    });

    // Regenerate embeddings if title or description changed
    if (title || description) {
      generateAndStoreEmbeddings(
        item.id,
        item.title,
        item.description,
        images
      ).catch((err) =>
        console.error("Embedding update failed:", err.message)
      );
    }

    res.json({ item });
  } catch (error) {
    console.error("Update item error:", error.message);
    res.status(500).json({ error: "Failed to update item" });
  }
}

// ── Delete own item ──────────────────────────
async function deleteItem(req, res) {
  try {
    const itemId = parseInt(req.params.id);

    const existing = await prisma.item.findUnique({ where: { id: itemId } });
    if (!existing) return res.status(404).json({ error: "Item not found" });
    if (existing.userId !== req.user.id) {
      return res
        .status(403)
        .json({ error: "You can only delete your own items" });
    }

    await prisma.item.delete({ where: { id: itemId } });

    res.json({ message: "Item deleted" });
  } catch (error) {
    console.error("Delete item error:", error.message);
    res.status(500).json({ error: "Failed to delete item" });
  }
}

// ── Mark item as claimed ─────────────────────
async function markAsClaimed(req, res) {
  try {
    const itemId = parseInt(req.params.id);

    const existing = await prisma.item.findUnique({ where: { id: itemId } });
    if (!existing) return res.status(404).json({ error: "Item not found" });
    if (existing.userId !== req.user.id) {
      return res
        .status(403)
        .json({ error: "You can only claim your own items" });
    }

    const item = await prisma.item.update({
      where: { id: itemId },
      data: { status: "CLAIMED" },
    });

    res.json({ item });
  } catch (error) {
    console.error("Claim item error:", error.message);
    res.status(500).json({ error: "Failed to claim item" });
  }
}

// ── Get similar items ──────────────────────────
async function getSimilarItems(req, res) {
  try {
    const itemId = parseInt(req.params.id);

    const existing = await prisma.item.findUnique({ where: { id: itemId } });
    if (!existing) return res.status(404).json({ error: "Item not found" });

    // Find similar items of the opposite status (e.g. if LOST, find FOUND)
    const searchAgainst = existing.status === "FOUND" ? "LOST" : "FOUND";
    
    // We try to find items similar by image first (if available), then by text
    let matches = [];
    try {
      matches = await findSimilarItems(itemId, searchAgainst, "text");
    } catch (err) {
      console.error("Vector search failed:", err.message);
    }

    res.json({ matches });
  } catch (error) {
    console.error("Get similar items error:", error.message);
    res.status(500).json({ error: "Failed to fetch similar items" });
  }
}

module.exports = {
  createItem,
  getAllItems,
  getItemById,
  updateItem,
  deleteItem,
  markAsClaimed,
  getSimilarItems,
};
