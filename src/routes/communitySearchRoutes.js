const express = require("express");
const router = express.Router();
const multer = require("multer");
const path = require("path");
const searchController = require("../controllers/communitySearchController");
const { requireAuth } = require("../middleware/authMiddleware");

// Separate Multer instance for search — accepts a single image
const searchUpload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      cb(null, path.join(__dirname, "../uploads"));
    },
    filename: (req, file, cb) => {
      const uniqueName = `community-search-${Date.now()}${path.extname(file.originalname)}`;
      cb(null, uniqueName);
    },
  }),
  limits: { fileSize: 5 * 1024 * 1024 },
});

router.post("/search/text", requireAuth, searchController.textSearch);
router.post("/search/image", requireAuth, searchUpload.single("image"), searchController.imageSearch);
router.post("/search/rag", requireAuth, searchController.ragSearch);

module.exports = router;
