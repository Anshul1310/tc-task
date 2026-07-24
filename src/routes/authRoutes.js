
const express = require("express");
const router = express.Router();

const authController = require("../controllers/authController");
const { requireAuth } = require("../middleware/authMiddleware");

router.get("/login", authController.login);
router.get("/callback", authController.callback);
router.post("/logout", requireAuth, authController.logout);
router.get("/me", requireAuth, authController.getCurrentUser);

module.exports = router;
