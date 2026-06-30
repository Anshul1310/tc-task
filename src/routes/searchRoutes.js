// ──────────────────────────────────────────────
// Search Routes
//
// POST /search/text  → Text-based similarity search
// POST /search/image → Image-based similarity search
// ──────────────────────────────────────────────

const express = require("express");
const multer = require("multer");
const path = require("path");
const router = express.Router();

const searchController = require("../controllers/searchController");

// Separate Multer instance for search — accepts a single image
const searchUpload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      cb(null, path.join(__dirname, "../uploads"));
    },
    filename: (req, file, cb) => {
      const uniqueName = `search-${Date.now()}${path.extname(file.originalname)}`;
      cb(null, uniqueName);
    },
  }),
  limits: { fileSize: 5 * 1024 * 1024 },
});

router.post("/text", searchController.textSearch);
router.post("/image", searchUpload.single("image"), searchController.imageSearch);

module.exports = router;
