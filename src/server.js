// ──────────────────────────────────────────────
// Server Entry Point
//
// This file starts the HTTP server.
// Run with: node src/server.js
// Or:       npm run dev (uses nodemon for auto-reload)
// ──────────────────────────────────────────────

const app = require("./app");
const prisma = require("./config/db");

const PORT = process.env.PORT || 3000;

async function startServer() {
  try {
    // 1. Test database connection
    await prisma.$connect();
    console.log("✅ Successfully connected to PostgreSQL database");

    // 2. Start the HTTP server
    app.listen(PORT, "0.0.0.0", () => {
      console.log(`🚀 Server running on port ${PORT}`);
    });
  } catch (error) {
    console.error("❌ Failed to connect to the database:", error.message);
    process.exit(1);
  }
}

startServer();