// ──────────────────────────────────────────────
// Express App Setup
//
// This file creates and configures the Express app.
// It does NOT start the server — that's server.js.
// Separating them makes testing easier.
// ──────────────────────────────────────────────

const express = require("express");
const cors = require("cors");
const path = require("path");

// Load environment variables from .env (in the root folder)
require("dotenv").config({ path: path.join(__dirname, "../.env") });

const app = express();

// ── Middleware ─────────────────────────────────

// Parse JSON request bodies
app.use(express.json());

// Parse URL-encoded form data
app.use(express.urlencoded({ extended: true }));

// Allow cross-origin requests (for frontend)
app.use(cors());

// Serve uploaded images as static files
// e.g., GET /uploads/abc123.jpg
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

// ── Health Check ──────────────────────────────

app.get("/health", (req, res) => {
  res.json({ status: "ok", timestamp: new Date().toISOString() });
});

// ── Routes ────────────────────────────────────

const authRoutes = require("./routes/authRoutes");
const discussionRoutes = require("./routes/discussionRoutes");
const commentRoutes = require("./routes/commentRoutes");
const communitySearchRoutes = require("./routes/communitySearchRoutes");
const locationRoutes = require("./routes/locationRoutes");

app.use("/auth", authRoutes);
app.use("/discussions", discussionRoutes);
app.use("/", commentRoutes);
app.use("/community", communitySearchRoutes);
app.use("/location", locationRoutes);

// ── Error Handler ─────────────────────────────
// Catches any unhandled errors from routes/middleware
app.use((err, req, res, next) => {
  console.error("Unhandled error:", err.message);

  // Multer errors (file too large, wrong type, etc.)
  if (err.name === "MulterError") {
    return res.status(400).json({ error: err.message });
  }

  res.status(500).json({ error: "Internal server error" });
});

module.exports = app;
