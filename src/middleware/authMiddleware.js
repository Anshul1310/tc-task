// ──────────────────────────────────────────────
// Auth Middleware
//
// Checks if the user is logged in by looking at
// the session and verifying the user exists in DB.
// If invalid or missing, returns 401.
// ──────────────────────────────────────────────

const prisma = require("../config/db");

async function requireAuth(req, res, next) {
  if (!req.session || !req.session.user) {
    return res.status(401).json({ error: "Not authenticated. Please log in." });
  }

  try {
    // Verify user exists in the database
    const user = await prisma.user.findUnique({
      where: { id: req.session.user.id },
      select: { id: true, email: true, name: true, anonymousUsername: true, avatarColor: true }
    });

    if (!user) {
      if (req.session) {
        req.session.destroy();
      }
      return res.status(401).json({ error: "User session expired or invalid. Please log in again." });
    }

    // Attach valid database user to request
    req.user = user;
    next();
  } catch (error) {
    console.error("Auth middleware error:", error.message);
    res.status(500).json({ error: "Authentication check failed" });
  }
}

module.exports = { requireAuth };
