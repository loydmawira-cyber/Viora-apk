package com.example.model

enum class TaskStatus { OPEN, IN_PROGRESS, DONE }
enum class TaskPriority { LOW, NORMAL, URGENT }

data class ModerationTask(
    val id: String,
    val title: String,
    val details: String,
    val reportId: String? = null,
    val assignedToId: String,
    val assignedToName: String,
    val assignedByName: String,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.OPEN,
    val note: String = "",
    val createdMs: Long = System.currentTimeMillis()
)

enum class AccountStatus { ACTIVE, MUTED, SUSPENDED, BANNED }

data class PlatformUser(
    val id: String,
    val name: String,
    val role: UserRole = UserRole.VIEWER,
    val strikes: Int = 0,
    val status: AccountStatus = AccountStatus.ACTIVE,
    val coinBalance: Long = 0,
    val reportCount: Int = 0
)

enum class AppealStatus { PENDING, APPROVED, REJECTED }

data class Appeal(
    val id: String,
    val userId: String,
    val userName: String,
    val reason: String,
    val status: AppealStatus = AppealStatus.PENDING,
    val createdMs: Long = System.currentTimeMillis()
)

data class AuditEntry(
    val id: String,
    val actorName: String,
    val actorRole: UserRole,
    val action: String,
    val target: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class ModeratorInfo(
    val id: String,
    val name: String,
    val isOnline: Boolean
)

enum class PurchaseStatus { COMPLETED, REFUNDED, FLAGGED }

data class PurchaseRecord(
    val id: String,
    val userName: String,
    val coins: Long,
    val usd: Double,
    val status: PurchaseStatus = PurchaseStatus.COMPLETED,
    val timestampMs: Long = System.currentTimeMillis()
)

data class FraudAlert(
    val id: String,
    val userName: String,
    val reason: String,
    val severity: String, // "HIGH", "MEDIUM"
    val resolved: Boolean = false
)

enum class ApplicationStatus { PENDING, APPROVED, REJECTED }

data class CreatorApplication(
    val id: String,
    val userId: String,
    val userName: String,
    val pitch: String,
    val status: ApplicationStatus = ApplicationStatus.PENDING
)

data class Announcement(
    val id: String,
    val title: String,
    val body: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class HeldMessage(
    val id: String,
    val streamId: String,
    val senderName: String,
    val content: String,
    val matchedKeyword: String
)

data class PlatformSettings(
    val giftingPaused: Boolean = false,
    val streamsPaused: Boolean = false
)

val CANNED_RESPONSES = listOf(
    "Please keep chat respectful. Harassment is not allowed on Viora.",
    "Your content was removed for breaking our Community Guidelines.",
    "Promotional links and spam are not allowed in live rooms.",
    "This is a formal warning. Further violations may lead to suspension.",
    "We could not find a violation in this report. No action was taken."
)

val DEFAULT_BANNED_KEYWORDS = listOf("scam", "free coins", "send money", "kill yourself")

// Withdrawal safety rules
const val MIN_WITHDRAWAL_DIAMONDS = 500L          // $5.00
const val LARGE_WITHDRAWAL_DIAMONDS = 50_000L     // $500.00 needs a second admin
const val NEW_CREATOR_HOLD_DAYS = 7L
