// ──────────────────────────────────────────────
// Server Entry Point
//
// This file starts the HTTP server.
// Run with: node src/server.js
// Or:       npm run dev (uses nodemon for auto-reload)
// ──────────────────────────────────────────────

const app = require("./app");

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`🚀 Server running on http://localhost:${PORT}`);
  console.log(`📋 Health check: http://localhost:${PORT}/health`);
});
