package com.example.model

enum class UserRole {
    VIEWER,
    BROADCASTER,
    MODERATOR,
    ADMIN
}

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bio: String = "",
    val role: UserRole = UserRole.VIEWER,
    val coinBalance: Long = 500,
    val diamondBalance: Long = 120, // 100 diamonds = $1.00 USD
    val followersCount: Int = 1420,
    val followingCount: Int = 230,
    val isVerified: Boolean = false,
    val level: Int = 12
)
