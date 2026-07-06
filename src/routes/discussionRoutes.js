const express = require("express");
const router = express.Router();
const discussionController = require("../controllers/discussionController");
const { requireAuth } = require("../middleware/authMiddleware");
const { uploadImages } = require("../middleware/uploadMiddleware");

// Public routes (if you want feed to be public)
// We will require auth for everything to ensure anonymous identities are used.
router.get("/", requireAuth, discussionController.getAllDiscussions);
router.get("/trending", requireAuth, discussionController.getTrendingDiscussions);
router.get("/:id", requireAuth, discussionController.getDiscussionById);

// Protected routes
router.post("/", requireAuth, uploadImages, discussionController.createDiscussion);
router.post("/:id/upvote", requireAuth, discussionController.toggleUpvote);

module.exports = router;
