package com.example.model

data class DirectMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false,
    val isRead: Boolean = true
)

data class Conversation(
    val id: String,
    val participantId: String,
    val participantName: String,
    val participantAvatar: String,
    val lastMessage: String,
    val lastMessageTimestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isCreator: Boolean = true,
    val verifiedBadge: Boolean = true
)
