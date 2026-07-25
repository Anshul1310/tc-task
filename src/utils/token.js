const jwt = require("jsonwebtoken");

const SECRET = process.env.JWT_SECRET || process.env.SESSION_SECRET || "campuscare-simple-token-secret-12345";

function generateToken(userId) {
  return jwt.sign({ userId }, SECRET, { expiresIn: "7d" });
}

function verifyToken(token) {
  try {
    return jwt.verify(token, SECRET);
  } catch (err) {
    return null;
  }
}

module.exports = { generateToken, verifyToken };
