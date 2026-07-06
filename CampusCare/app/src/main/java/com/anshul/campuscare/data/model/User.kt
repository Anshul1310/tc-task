package com.anshul.campuscare.data.model

// ──────────────────────────────────────────────
// User Data Classes
//
// Represents a user from the backend.
// The backend returns user info in different
// wrapper formats depending on the endpoint.
// ──────────────────────────────────────────────

/**
 * A single user as stored in the database.
 * Matches the User model from the backend Prisma schema.
 */
data class User(
    val id: Int,
    val email: String,
    val name: String
)

/**
 * Response wrapper for GET /auth/me
 * The backend returns: { "user": { id, email, name } }
 */
data class UserResponse(
    val user: User
)

/**
 * Response wrapper for GET /auth/callback
 * The backend returns: { "message": "...", "user": { id, email, name } }
 */
data class LoginResponse(
    val message: String,
    val user: User
)

/**
 * Response wrapper for POST /auth/logout
 * The backend returns: { "message": "Logged out successfully" }
 */
data class LogoutResponse(
    val message: String
)
