// ──────────────────────────────────────────────
// DAuth Service
//
// Wraps all communication with DAuth (auth.delta.nitt.edu).
// DAuth is NIT Trichy's OAuth2 SSO. This file handles:
//   1. Building the login URL (where we redirect the user)
//   2. Exchanging an auth code for an access token
//   3. Fetching the user's profile with the access token
// ──────────────────────────────────────────────

const axios = require("axios");

const DAUTH_BASE_URL = "https://auth.delta.nitt.edu";

// Step 1: Build the URL that starts the OAuth2 login flow.
// We redirect the user's browser to this URL.
function getAuthorizationUrl() {
  const params = new URLSearchParams({
    client_id: process.env.DAUTH_CLIENT_ID,
    redirect_uri: process.env.DAUTH_REDIRECT_URI,
    response_type: "code",
    grant_type: "authorization_code",
    scope: "email openid profile user",
    state: "lost-and-found", // Prevents CSRF attacks
    nonce: Date.now().toString(),
  });

  return `${DAUTH_BASE_URL}/authorize?${params.toString()}`;
}

// Step 2: Exchange the authorization code for an access token.
// This is a server-to-server call (the user never sees it).
async function exchangeCodeForToken(code) {
  const response = await axios.post(
    `${DAUTH_BASE_URL}/api/oauth/token`,
    new URLSearchParams({
      client_id: process.env.DAUTH_CLIENT_ID,
      client_secret: process.env.DAUTH_CLIENT_SECRET,
      grant_type: "authorization_code",
      code,
      redirect_uri: process.env.DAUTH_REDIRECT_URI,
    }).toString(),
    {
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
    }
  );

  return response.data; // { access_token, token_type, ... }
}

// Step 3: Use the access token to fetch the user's profile.
async function getUserProfile(accessToken) {
  const response = await axios.post(
    `${DAUTH_BASE_URL}/api/resources/user`,
    {},
    {
      headers: { Authorization: `Bearer ${accessToken}` },
    }
  );

  return response.data; // { email, name, ... }
}

module.exports = {
  getAuthorizationUrl,
  exchangeCodeForToken,
  getUserProfile,
};
