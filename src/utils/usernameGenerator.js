const prisma = require("../config/db");
const crypto = require("crypto");

const ADJECTIVES = [
  "Flying", "Silent", "Cosmic", "Blue", "Crimson",
  "Rapid", "Golden", "Silver", "Misty", "Shadow",
  "Neon", "Cyber", "Lunar", "Solar", "Echo",
  "Phantom", "Velvet", "Onyx", "Ruby", "Sapphire"
];

const NOUNS = [
  "Fox", "Tiger", "Falcon", "Wolf", "Hawk",
  "Panda", "Otter", "Eagle", "Bear", "Koala",
  "Panther", "Lion", "Leopard", "Owl", "Raven",
  "Lynx", "Viper", "Cobra", "Dragon", "Phoenix"
];

const AVATAR_COLORS = ["🟦", "🟩", "🟨", "🟥", "🟪", "🟧", "🟫"];

// Helper to get a random item from an array
function getRandom(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

/**
 * Generate a random base username (e.g., FlyingFox).
 */
function generateBaseUsername() {
  const adj = getRandom(ADJECTIVES);
  const noun = getRandom(NOUNS);
  return `${adj}${noun}`;
}

/**
 * Check if a username exists in the DB.
 */
async function usernameExists(username) {
  const user = await prisma.user.findUnique({
    where: { anonymousUsername: username },
  });
  return !!user;
}

/**
 * Generate a unique username.
 * If a collision occurs, append a short random suffix (e.g., FlyingFox-7Q).
 */
async function generateUniqueUsername() {
  let username = generateBaseUsername();
  
  if (!(await usernameExists(username))) {
    return username;
  }

  // Handle collision
  while (true) {
    const randomSuffix = crypto.randomBytes(1).toString("hex").toUpperCase(); // e.g., "A3"
    const suffixLetters = String.fromCharCode(65 + Math.floor(Math.random() * 26)); // Random letter A-Z
    const suffixNum = Math.floor(Math.random() * 9) + 1; // Random number 1-9
    
    const candidate = `${username}-${suffixLetters}${suffixNum}`;
    
    if (!(await usernameExists(candidate))) {
      return candidate;
    }
  }
}

/**
 * Return a random avatar color emoji.
 */
function getRandomAvatarColor() {
  return getRandom(AVATAR_COLORS);
}

module.exports = {
  generateUniqueUsername,
  getRandomAvatarColor,
};
