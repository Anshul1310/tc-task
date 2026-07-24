package com.anshul.campuscare.data.model


data class User(
    val id: Int,
    val email: String,
    val name: String,
    val anonymousUsername: String?,
    val avatarColor: String?,
    val createdAt: String
)


data class UserResponse(
    val user: User
)



data class LogoutResponse(
    val message: String
)
