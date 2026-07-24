// ──────────────────────────────────────────────
// Token Helper (HMAC Signed Tokens)
//
// Simple, zero-dependency token generator & verifier
// using Node.js built-in crypto module.
// ──────────────────────────────────────────────

const crypto = require("crypto");

const SECRET = process.env.SESSION_SECRET || "campuscare-simple-token-secret-12345";

/**
 * Generates a signed token string for a given userId.
 */
function generateToken(userId) {
  const payload = Buffer.from(JSON.stringify({ userId, created: Date.now() })).toString("base64url");
  const signature = crypto.createHmac("sha256", SECRET).update(payload).digest("base64url");
  return `${payload}.${signature}`;
}

/**
 * Verifies a token string and returns decoded payload { userId, created } or null.
 */
function verifyToken(token) {
  try {
    if (!token || typeof token !== "string") return null;
    const parts = token.split(".");
    if (parts.length !== 2) return null;

    const [payload, signature] = parts;
    const expectedSignature = crypto.createHmac("sha256", SECRET).update(payload).digest("base64url");

    if (signature !== expectedSignature) return null;

    const data = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
    return data;
  } catch (err) {
    return null;
  }
}

module.exports = { generateToken, verifyToken };
