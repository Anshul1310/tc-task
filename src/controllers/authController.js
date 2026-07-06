// ──────────────────────────────────────────────
// Auth Controller
//
// Handles the DAuth OAuth2 login flow:
//   GET  /auth/login    → Redirects to DAuth
//   GET  /auth/callback → DAuth redirects back here
//   POST /auth/logout   → Destroys session
//   GET  /auth/me       → Returns current user
// ──────────────────────────────────────────────

const prisma = require("../config/db");
const dauthService = require("../services/dauthService");
const { generateUniqueUsername, getRandomAvatarColor } = require("../utils/usernameGenerator");

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

    // Check if user exists first to avoid generating names unnecessarily
    let user = await prisma.user.findUnique({
      where: { email: profile.email },
    });

    if (user) {
      // Update existing user
      user = await prisma.user.update({
        where: { id: user.id },
        data: { name: profile.name },
      });
    } else {
      // Create new user with anonymous identity
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

    // Store user info in session (this is how we "remember" the login)
    req.session.user = {
      id: user.id,
      email: user.email,
      name: user.name,
      anonymousUsername: user.anonymousUsername,
      avatarColor: user.avatarColor,
    };

    res.json({
      message: "Login successful",
      user: req.session.user,
    });
  } catch (error) {
    console.error("Auth callback error:", error.message);
    res.status(500).json({ error: "Authentication failed" });
  }
}

// Destroy the session → user is logged out
async function logout(req, res) {
  req.session.destroy((err) => {
    if (err) {
      return res.status(500).json({ error: "Logout failed" });
    }
    res.json({ message: "Logged out successfully" });
  });
}

// Return the currently logged-in user's info
async function getCurrentUser(req, res) {
  // req.user is set by the authMiddleware
  res.json({ user: req.user });
}

module.exports = { login, callback, logout, getCurrentUser };
