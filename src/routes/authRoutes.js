// ──────────────────────────────────────────────
// Auth Routes
//
// GET  /auth/login    → Start DAuth login
// GET  /auth/callback → DAuth redirects here
// POST /auth/logout   → End session
// GET  /auth/me       → Get current user (protected)
// ──────────────────────────────────────────────

const express = require("express");
const router = express.Router();

const authController = require("../controllers/authController");
const { requireAuth } = require("../middleware/authMiddleware");

router.get("/login", authController.login);
router.get("/callback", authController.callback);
router.post("/logout", requireAuth, authController.logout);
router.get("/me", requireAuth, authController.getCurrentUser);

module.exports = router;
