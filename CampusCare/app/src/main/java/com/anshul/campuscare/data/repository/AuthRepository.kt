package com.anshul.campuscare.data.repository

// ──────────────────────────────────────────────
// Auth Repository
//
// Handles authentication operations. Wraps the
// API service calls and returns Result<T> so the
// UI can handle success and failure easily.
//
// This is an object (singleton) — no dependency
// injection needed. Just call AuthRepository.getCurrentUser().
// ──────────────────────────────────────────────

import com.anshul.campuscare.data.model.User
import com.anshul.campuscare.data.network.ApiClient

object AuthRepository {

    private val apiService = ApiClient.apiService

    // ── Auth ──────────────────────────────────

    /**
     * Check if the user is currently logged in by
     * calling GET /auth/me. Returns the user if the
     * session cookie is valid, null otherwise.
     */
    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                val userResponse = response.body()
                Result.success(userResponse?.user)
            } else {
                // 401 means not logged in — that's not an error
                Result.success(null)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Log out the current user.
     */
    suspend fun logout(): Result<String> {
        return try {
            val response = apiService.logout()
            if (response.isSuccessful) {
                ApiClient.clearSession()
                Result.success("Logged out successfully")
            } else {
                Result.failure(Exception("Logout failed"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
