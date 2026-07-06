// ──────────────────────────────────────────────
// Express App Setup
//
// This file creates and configures the Express app.
// It does NOT start the server — that's server.js.
// Separating them makes testing easier.
// ──────────────────────────────────────────────

const express = require("express");
const cors = require("cors");
const session = require("express-session");
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

// Session middleware — stores login state
app.use(
  session({
    secret: process.env.SESSION_SECRET || "dev-secret",
    resave: false,
    saveUninitialized: false,
    cookie: {
      maxAge: 24 * 60 * 60 * 1000, // 24 hours
      httpOnly: true,
    },
  })
);

// Serve uploaded images as static files
// e.g., GET /uploads/abc123.jpg
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

// ── Health Check ──────────────────────────────

app.get("/health", (req, res) => {
  res.json({ status: "ok", timestamp: new Date().toISOString() });
});

// ── Routes ────────────────────────────────────

const authRoutes = require("./routes/authRoutes");
const itemRoutes = require("./routes/itemRoutes");
const searchRoutes = require("./routes/searchRoutes");
const discussionRoutes = require("./routes/discussionRoutes");
const commentRoutes = require("./routes/commentRoutes");
const notificationRoutes = require("./routes/notificationRoutes");
const communitySearchRoutes = require("./routes/communitySearchRoutes");

app.use("/auth", authRoutes);
app.use("/items", itemRoutes);
app.use("/search", searchRoutes);
app.use("/discussions", discussionRoutes);
app.use("/", commentRoutes);
app.use("/notifications", notificationRoutes);
app.use("/community", communitySearchRoutes);

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
