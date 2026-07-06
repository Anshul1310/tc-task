// ──────────────────────────────────────────────
// Item Routes
//
// RESTful CRUD for lost and found items.
// All routes except GET require authentication.
//
// GET    /items          → List all items (public)
// GET    /items/:id      → Get one item (public)
// POST   /items          → Create item (auth + upload)
// PUT    /items/:id      → Update item (auth + upload)
// DELETE /items/:id      → Delete item (auth)
// PATCH  /items/:id/claim → Mark as claimed (auth)
// ──────────────────────────────────────────────

const express = require("express");
const router = express.Router();

const itemController = require("../controllers/itemController");
const { requireAuth } = require("../middleware/authMiddleware");
const { uploadImages } = require("../middleware/uploadMiddleware");

// Public routes
router.get("/", itemController.getAllItems);
router.get("/:id", itemController.getItemById);
router.get("/:id/similar", itemController.getSimilarItems);

// Protected routes (must be logged in)
router.post("/", requireAuth, uploadImages, itemController.createItem);
router.put("/:id", requireAuth, uploadImages, itemController.updateItem);
router.delete("/:id", requireAuth, itemController.deleteItem);
router.patch("/:id/claim", requireAuth, itemController.markAsClaimed);

module.exports = router;
