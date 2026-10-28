package com.example.model

enum class MessageType {
    CHAT,
    SYSTEM,
    GIFT_ANNOUNCEMENT,
    PINNED
}

data class ChatMessage(
    val id: String,
    val streamId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val senderRole: UserRole = UserRole.VIEWER,
    val content: String,
    val type: MessageType = MessageType.CHAT,
    val timestampMs: Long = System.currentTimeMillis(),
    val isBroadcaster: Boolean = false,
    val isModerator: Boolean = false,
    val userLevel: Int = 5,
    val isDeleted: Boolean = false
)
