// ──────────────────────────────────────────────
// Auth Middleware
//
// Checks if the user is logged in by looking at
// the session. If not logged in, returns 401.
//
// Usage: router.get("/protected", requireAuth, handler)
// ──────────────────────────────────────────────

function requireAuth(req, res, next) {
  if (!req.session || !req.session.user) {
    return res.status(401).json({ error: "Not authenticated. Please log in." });
  }

  // Attach user to request for easy access in controllers
  req.user = req.session.user;
  next();
}

module.exports = { requireAuth };
