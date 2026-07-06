const express = require("express");
const router = express.Router();
const commentController = require("../controllers/commentController");
const { requireAuth } = require("../middleware/authMiddleware");
const { uploadImages } = require("../middleware/uploadMiddleware");

// Note: Multer middleware expects the field name "images" currently, or we can use a generic "image".
// The existing uploadImages uses .array("images", 5).
// We'll use uploadImages for comments as well but expect only one image from the frontend.

// POST /discussions/:id/comments
router.post("/discussions/:id/comments", requireAuth, uploadImages, commentController.addComment);

// POST /comments/:id/replies
router.post("/comments/:id/replies", requireAuth, commentController.addReply);

module.exports = router;
