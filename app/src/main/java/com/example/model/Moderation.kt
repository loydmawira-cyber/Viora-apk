package com.example.model

enum class ReportCategory(val displayName: String) {
    INAPPROPRIATE_CONTENT("Inappropriate Content"),
    HARASSMENT("Harassment or Bullying"),
    SPAM("Spam or Scams"),
    COPYRIGHT("Copyright Infringement"),
    VIOLENCE("Violence or Harm"),
    UNDERAGE("Suspected Minor"),
    OTHER("Other Violation")
}

enum class ReportStatus {
    PENDING_REVIEW,
    DISMISSED,
    ACTION_TAKEN
}

data class ModerationReport(
    val id: String,
    val targetType: String, // "STREAM", "USER", "MESSAGE"
    val targetId: String,
    val targetName: String,
    val reportedBy: String,
    val category: ReportCategory,
    val notes: String,
    val status: ReportStatus = ReportStatus.PENDING_REVIEW,
    val timestampMs: Long = System.currentTimeMillis()
)
