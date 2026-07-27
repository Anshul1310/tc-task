

const prisma = require("../config/db");
const { verifyToken } = require("../utils/token");

async function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  let token = null;

  if (authHeader && authHeader.startsWith("Bearer ")) {
    token = authHeader.substring(7).trim();
  }

  if (!token) {
    return res.status(401).json({ error: "No authentication token provided. Please log in." });
  }

  const decoded = verifyToken(token);
  if (!decoded || !decoded.userId) {
    return res.status(401).json({ error: "Invalid or expired token. Please log in again." });
  }

  try {
    const user = await prisma.user.findUnique({
      where: { id: decoded.userId },
      select: { id: true, email: true, name: true, anonymousUsername: true, avatarColor: true }
    });

    if (!user) {
      return res.status(401).json({ error: "User no longer exists. Please log in again." });
    }

    req.user = user;
    next();
  } catch (error) {
    console.error("Auth middleware error:", error.message);
    res.status(500).json({ error: "Authentication check failed" });
  }
}

module.exports = { requireAuth };
