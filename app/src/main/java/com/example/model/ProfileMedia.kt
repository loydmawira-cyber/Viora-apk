package com.example.model

enum class MediaType {
    PHOTO,
    VIDEO
}

data class ProfileMedia(
    val id: String,
    val userId: String,
    val authorName: String,
    val authorAvatar: String,
    val mediaType: MediaType,
    val mediaUrl: String,
    val thumbnailUrl: String,
    val caption: String,
    val isLocked: Boolean = false,
    val coinPrice: Long = 0,
    val likesCount: Int = 142,
    val unlockedByUserIds: List<String> = emptyList(),
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
