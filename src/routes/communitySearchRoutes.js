const express = require("express");
const router = express.Router();
const searchController = require("../controllers/communitySearchController");
const { requireAuth } = require("../middleware/authMiddleware");

router.post("/search/text", requireAuth, searchController.textSearch);
router.post("/search/rag", requireAuth, searchController.ragSearch);

module.exports = router;
