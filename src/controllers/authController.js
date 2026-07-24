
const prisma = require("../config/db");
const dauthService = require("../services/dauthService");
const { generateUniqueUsername, getRandomAvatarColor } = require("../utils/usernameGenerator");
const { generateToken } = require("../utils/token");

async function login(req, res) {
  const authUrl = dauthService.getAuthorizationUrl();
  res.redirect(authUrl);
}

async function callback(req, res) {
  try {
    const { code } = req.query;

    if (!code) {
      return res.status(400).json({ error: "Authorization code missing" });
    }

    const tokenData = await dauthService.exchangeCodeForToken(code);
    const accessToken = tokenData.access_token;

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

    const token = generateToken(user.id);

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
