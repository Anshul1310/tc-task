

const axios = require("axios");

const DAUTH_BASE_URL = "https://auth.delta.nitt.edu";


function getAuthorizationUrl() {
  console.log("\n=== DAUTH CONFIGURATION ===");
  console.log("CLIENT_ID:", process.env.DAUTH_CLIENT_ID);
  console.log("REDIRECT_URI:", process.env.DAUTH_REDIRECT_URI);
  console.log("SECRET (length):", process.env.DAUTH_CLIENT_SECRET ? process.env.DAUTH_CLIENT_SECRET.length : 0);
  console.log("===========================\n");

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


async function exchangeCodeForToken(code) {
  console.log("\n=== DAUTH TOKEN EXCHANGE ===");
  console.log("CLIENT_ID sent:", process.env.DAUTH_CLIENT_ID);
  console.log("REDIRECT_URI sent:", process.env.DAUTH_REDIRECT_URI);
  console.log("==============================\n");

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

  return response.data;
}

async function getUserProfile(accessToken) {
  const response = await axios.post(
    `${DAUTH_BASE_URL}/api/resources/user`,
    {},
    {
      headers: { Authorization: `Bearer ${accessToken}` },
    }
  );

  return response.data;
}

module.exports = {
  getAuthorizationUrl,
  exchangeCodeForToken,
  getUserProfile,
};
