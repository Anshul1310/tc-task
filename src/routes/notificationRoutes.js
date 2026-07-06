const express = require("express");
const router = express.Router();
const notificationController = require("../controllers/notificationController");
const { requireAuth } = require("../middleware/authMiddleware");

// GET /notifications
router.get("/", requireAuth, notificationController.getUserNotifications);

// PATCH /notifications/:id/read
router.patch("/:id/read", requireAuth, notificationController.markAsRead);

module.exports = router;
