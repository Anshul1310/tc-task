// ──────────────────────────────────────────────
// Auth Controller
//
// Handles the DAuth OAuth2 login flow:
//   GET  /auth/login    → Redirects to DAuth
//   GET  /auth/callback → Exchange code, return JWT token & user
//   POST /auth/logout   → Clears login response
//   GET  /auth/me       → Returns current authenticated user
// ──────────────────────────────────────────────

const prisma = require("../config/db");
const dauthService = require("../services/dauthService");
const { generateUniqueUsername, getRandomAvatarColor } = require("../utils/usernameGenerator");
const { generateToken } = require("../utils/token");

// Redirect the user to DAuth's login page
async function login(req, res) {
  const authUrl = dauthService.getAuthorizationUrl();
  res.redirect(authUrl);
}

// DAuth redirects the user back here with ?code=xxx
async function callback(req, res) {
  try {
    const { code } = req.query;

    if (!code) {
      return res.status(400).json({ error: "Authorization code missing" });
    }

    // Exchange code for access token (server-to-server)
    const tokenData = await dauthService.exchangeCodeForToken(code);
    const accessToken = tokenData.access_token;

    // Fetch user profile from DAuth
    const profile = await dauthService.getUserProfile(accessToken);

    let user = await prisma.user.findUnique({
      where: { email: profile.email },
    });

    if (user) {
      user = await prisma.user.update({
        where: { id: user.id },
        data: { name: profile.name },
      });
    } else {
      const anonymousUsername = await generateUniqueUsername();
      const avatarColor = getRandomAvatarColor();

      user = await prisma.user.create({
        data: {
          email: profile.email,
          name: profile.name,
          anonymousUsername,
          avatarColor,
        },
      });
    }

    // Generate Token
    const token = generateToken(user.id);

    // Return HTML page with token so Android WebView can extract it cleanly
    res.send(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Login Successful</title>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
          <h2 style="color: #2e7d32;">Login Successful!</h2>
          <p>Redirecting to CampusCare...</p>
          <div id="token" style="font-weight: bold; word-break: break-all; padding: 10px; background: #f0f0f0;">${token}</div>
        </body>
      </html>
    `);
  } catch (error) {
    console.error("Auth callback error:", error.message);
    res.status(500).json({ error: "Authentication failed" });
  }
}

async function logout(req, res) {
  res.json({ message: "Logged out successfully" });
}

async function getCurrentUser(req, res) {
  res.json({ user: req.user });
}

module.exports = { login, callback, logout, getCurrentUser };
