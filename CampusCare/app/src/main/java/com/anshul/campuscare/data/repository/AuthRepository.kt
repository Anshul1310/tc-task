package com.anshul.campuscare.data.repository

import com.anshul.campuscare.data.model.User
import com.anshul.campuscare.data.network.ApiClient

object AuthRepository {

    private val apiService = ApiClient.apiService

    suspend fun fetchAndSaveCurrentUser(): Result<User?> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                val userResponse = response.body()
                if (userResponse != null) {
                    val user = userResponse.user
                    ApiClient.saveUser(user)
                    Result.success(user)
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getCurrentUser(): Result<User?> {
        val savedUser = ApiClient.getUser()
        if (savedUser != null) {
            return Result.success(savedUser)
        }
        return fetchAndSaveCurrentUser()
    }
}
