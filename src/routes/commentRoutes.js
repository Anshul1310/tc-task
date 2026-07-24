const express = require("express");
const router = express.Router();
const commentController = require("../controllers/commentController");
const { requireAuth } = require("../middleware/authMiddleware");

router.post("/discussions/:id/comments", requireAuth, commentController.addComment);

module.exports = router;
