package com.anshul.campuscare.data.repository


import com.anshul.campuscare.data.model.User
import com.anshul.campuscare.data.network.ApiClient

object AuthRepository {

    private val apiService = ApiClient.apiService

    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                val userResponse = response.body()
                Result.success(userResponse?.user)
            } else {
                Result.success(null)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }


}
