package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.example.BuildConfig

object VioraRepository {

    private val _currentUser = MutableStateFlow(MockDataProvider.currentUser)
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // With a real backend configured the app starts signed out and restores the saved session.
    // Without keys it stays in demo mode.
    private val _isAuthenticated = MutableStateFlow(!SupabaseApi.isConfigured)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isRestoring = MutableStateFlow(SupabaseApi.isConfigured)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _liveStreams = MutableStateFlow(MockDataProvider.initialStreams)
    val liveStreams: StateFlow<List<LiveStream>> = _liveStreams.asStateFlow()

    private val _chatMessagesMap = MutableStateFlow<Map<String, List<ChatMessage>>>(
        MockDataProvider.initialStreams.associate { it.id to MockDataProvider.sampleChatMessages(it.id) }
    )
    val chatMessagesMap: StateFlow<Map<String, List<ChatMessage>>> = _chatMessagesMap.asStateFlow()

    private val _activeGiftBanner = MutableStateFlow<GiftTransaction?>(null)
    val activeGiftBanner: StateFlow<GiftTransaction?> = _activeGiftBanner.asStateFlow()

    private val _ledger = MutableStateFlow(MockDataProvider.initialLedger)
    val ledger: StateFlow<List<LedgerTransaction>> = _ledger.asStateFlow()

    private val _withdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val withdrawals: StateFlow<List<WithdrawalRequest>> = _withdrawals.asStateFlow()

    private val seedReports: List<ModerationReport> = (
        MockDataProvider.initialReports + ModerationReport(
            "rep_03", "MEDIA", "media_03", "Reported creator video", "viewer_2210",
            ReportCategory.INAPPROPRIATE_CONTENT, "Video thumbnail looks inappropriate",
            ReportStatus.PENDING_REVIEW, System.currentTimeMillis() - 900000
        ) + listOf(
            ModerationReport("rep_h1", "USER", "usr_90", "spam_bot_1", "viewer_1", ReportCategory.SPAM, "Link spam", ReportStatus.ACTION_TAKEN,
                System.currentTimeMillis() - 5 * 3600000L, resolvedByName = "Marcus", resolvedMs = System.currentTimeMillis() - 5 * 3600000L + 6 * 60000L),
            ModerationReport("rep_h2", "STREAM", "stream_x", "Old stream", "viewer_2", ReportCategory.HARASSMENT, "Insults", ReportStatus.DISMISSED,
                System.currentTimeMillis() - 8 * 3600000L, resolvedByName = "Marcus", resolvedMs = System.currentTimeMillis() - 8 * 3600000L + 10 * 60000L),
            ModerationReport("rep_h3", "MESSAGE", "msg_x", "chat_user_7", "viewer_3", ReportCategory.VIOLENCE, "Threats", ReportStatus.ACTION_TAKEN,
                System.currentTimeMillis() - 20 * 3600000L, resolvedByName = "Sara", resolvedMs = System.currentTimeMillis() - 20 * 3600000L + 12 * 60000L)
        )
    ).map { r ->
        if (r.id == "rep_01") r.copy(assignedToName = "Alex Rivers", assignedToId = "usr_me_01") else r
    }

    private val _reports = MutableStateFlow(seedReports)
    val reports: StateFlow<List<ModerationReport>> = _reports.asStateFlow()

    private val _profileMedia = MutableStateFlow(MockDataProvider.initialProfileMedia)
    val profileMedia: StateFlow<List<ProfileMedia>> = _profileMedia.asStateFlow()

    private val _mutedUsers = MutableStateFlow<Set<String>>(emptySet())
    val mutedUsers: StateFlow<Set<String>> = _mutedUsers.asStateFlow()

    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    private val _conversations = MutableStateFlow(
        listOf(
            Conversation(
                id = "conv_01",
                participantId = "usr_02",
                participantName = "Chloe Beats",
                participantAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                lastMessage = "Thank you so much for the Sports Car! 🏎️ What song should I play next live?",
                lastMessageTimestamp = "10m ago",
                unreadCount = 1,
                isOnline = true,
                isCreator = true,
                verifiedBadge = true
            ),
            Conversation(
                id = "conv_02",
                participantId = "usr_03",
                participantName = "Neon K",
                participantAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                lastMessage = "Sub-45 split run replay is live! Did you catch that glitch shortcut?",
                lastMessageTimestamp = "2h ago",
                unreadCount = 0,
                isOnline = true,
                isCreator = true,
                verifiedBadge = true
            ),
            Conversation(
                id = "conv_03",
                participantId = "usr_04",
                participantName = "Maya Lin",
                participantAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                lastMessage = "K-Pop workshop scheduled for tomorrow at 6 PM KST ✨",
                lastMessageTimestamp = "5h ago",
                unreadCount = 2,
                isOnline = false,
                isCreator = true,
                verifiedBadge = true
            ),
            Conversation(
                id = "conv_04",
                participantId = "usr_05",
                participantName = "Sam Steele",
                participantAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                lastMessage = "Get ready for the core shred session tomorrow morning! 💪",
                lastMessageTimestamp = "1d ago",
                unreadCount = 0,
                isOnline = false,
                isCreator = true,
                verifiedBadge = false
            ),
            Conversation(
                id = "conv_05",
                participantId = "usr_sys",
                participantName = "Viora Team",
                participantAvatar = "",
                lastMessage = "Welcome to Viora! Check out your Creator Wallet to view earned Diamonds 💎",
                lastMessageTimestamp = "2d ago",
                unreadCount = 0,
                isOnline = true,
                isCreator = false,
                verifiedBadge = true
            )
        )
    )
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _directMessagesMap = MutableStateFlow<Map<String, List<DirectMessage>>>(
        mapOf(
            "conv_01" to listOf(
                DirectMessage("dm_1", "conv_01", "usr_02", "Hey Alex! Thanks for tuning into the acoustic stream!", System.currentTimeMillis() - 3600000, isFromMe = false),
                DirectMessage("dm_2", "conv_01", "usr_me_01", "Loved the 90s indie covers! 🎸", System.currentTimeMillis() - 1800000, isFromMe = true),
                DirectMessage("dm_3", "conv_01", "usr_02", "Thank you so much for the Sports Car! 🏎️ What song should I play next live?", System.currentTimeMillis() - 600000, isFromMe = false)
            ),
            "conv_02" to listOf(
                DirectMessage("dm_4", "conv_02", "usr_03", "Yo! Did you see that final boss split?", System.currentTimeMillis() - 7200000, isFromMe = false),
                DirectMessage("dm_5", "conv_02", "usr_me_01", "Insane movement! New WR incoming for sure 🚀", System.currentTimeMillis() - 7100000, isFromMe = true),
                DirectMessage("dm_6", "conv_02", "usr_03", "Sub-45 split run replay is live! Did you catch that glitch shortcut?", System.currentTimeMillis() - 7000000, isFromMe = false)
            ),
            "conv_03" to listOf(
                DirectMessage("dm_7", "conv_03", "usr_04", "Hey there! Thanks for following the dance studio 💃", System.currentTimeMillis() - 24000000, isFromMe = false),
                DirectMessage("dm_8", "conv_03", "usr_04", "K-Pop workshop scheduled for tomorrow at 6 PM KST ✨", System.currentTimeMillis() - 18000000, isFromMe = false)
            ),
            "conv_04" to listOf(
                DirectMessage("dm_9", "conv_04", "usr_05", "Get ready for the core shred session tomorrow morning! 💪", System.currentTimeMillis() - 86400000, isFromMe = false)
            ),
            "conv_05" to listOf(
                DirectMessage("dm_10", "conv_05", "usr_sys", "Welcome to Viora! Check out your Creator Wallet to view earned Diamonds 💎", System.currentTimeMillis() - 172800000, isFromMe = false)
            )
        )
    )
    val directMessagesMap: StateFlow<Map<String, List<DirectMessage>>> = _directMessagesMap.asStateFlow()

    fun sendDirectMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        val newDm = DirectMessage(
            id = "dm_${UUID.randomUUID()}",
            conversationId = conversationId,
            senderId = _currentUser.value.id,
            text = text.trim(),
            timestampMs = System.currentTimeMillis(),
            isFromMe = true,
            isRead = true
        )

        _directMessagesMap.update { map ->
            val existing = map[conversationId] ?: emptyList()
            map + (conversationId to (existing + newDm))
        }

        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) {
                    conv.copy(
                        lastMessage = text.trim(),
                        lastMessageTimestamp = "Just now"
                    )
                } else conv
            }
        }
    }

    fun markConversationRead(conversationId: String) {
        _conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it }
        }
    }

    // Auth actions
    fun login(email: String, pass: String) {
        _isAuthenticated.value = true
    }

    fun signup(username: String, email: String, pass: String) {
        _currentUser.update {
            it.copy(
                username = username.ifBlank { "new_creator" },
                displayName = username.replaceFirstChar { char -> char.uppercase() }.ifBlank { "New Creator" }
            )
        }
        _isAuthenticated.value = true
    }

    // ---- Real backend (Supabase) -------------------------------------------------

    /** Call once from MainActivity. Restores a saved session when a backend is configured. */
    fun init(context: android.content.Context) {
        SupabaseApi.init(context)
        if (!SupabaseApi.isConfigured) {
            _isRestoring.value = false
            return
        }
        repoScope.launch {
            try {
                if (SupabaseApi.hasSession() && SupabaseApi.refresh()) loadAccount()
            } catch (_: Exception) {
                // Offline or expired: show the sign-in screen. The saved session stays for next time.
            } finally {
                _isRestoring.value = false
            }
        }
    }

    private suspend fun loadAccount() {
        val user = SupabaseApi.fetchMyUser()
        _currentUser.value = user
        _isAuthenticated.value = true
        if (user.isBanned) loadMyAppeals()
    }

    /** Re-reads role, wallet and ban status. Safe to call often. */
    fun refreshAccount() {
        if (!SupabaseApi.isConfigured || !_isAuthenticated.value) return
        repoScope.launch { runCatching { loadAccount() } }
    }

    suspend fun signIn(email: String, pass: String): Result<Unit> {
        if (!SupabaseApi.isConfigured) {
            login(email, pass)
            return Result.success(Unit)
        }
        return runCatching {
            SupabaseApi.signIn(email, pass)
            loadAccount()
        }
    }

    /** Success(true) = signed in. Success(false) = account created, email confirmation needed. */
    suspend fun signUp(username: String, email: String, pass: String): Result<Boolean> {
        if (!SupabaseApi.isConfigured) {
            signup(username, email, pass)
            return Result.success(true)
        }
        return runCatching {
            val signedIn = SupabaseApi.signUp(email, pass, username, username)
            if (signedIn) loadAccount()
            signedIn
        }
    }

    private suspend fun loadMyAppeals() {
        val uid = SupabaseApi.userId ?: return
        val rows = SupabaseApi.select("appeals?user_id=eq.$uid&select=*&order=created_at.desc")
        val mine = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.map { r ->
            Appeal(
                id = r.str("id"),
                userId = uid,
                userName = _currentUser.value.displayName,
                reason = r.str("reason"),
                status = when (r.str("status").lowercase()) {
                    "approved" -> AppealStatus.APPROVED
                    "rejected" -> AppealStatus.REJECTED
                    else -> AppealStatus.PENDING
                }
            )
        }
        _appeals.update { list -> list.filterNot { it.userId == uid } + mine }
    }

    fun logout() {
        if (BuildConfig.DEBUG) _currentUser.update { it.copy(isBanned = false) }
        _isAuthenticated.value = false
        if (SupabaseApi.isConfigured) {
            repoScope.launch { SupabaseApi.signOut() }
        }
    }

    fun updateProfile(displayName: String, bio: String) {
        _currentUser.update { it.copy(displayName = displayName, bio = bio) }
    }

    // Stream actions
    fun toggleFollowStream(streamId: String) {
        _liveStreams.update { list ->
            list.map { stream ->
                if (stream.id == streamId) {
                    val newFollowing = !stream.isFollowing
                    stream.copy(isFollowing = newFollowing)
                } else stream
            }
        }
    }

    fun addLike(streamId: String) {
        _liveStreams.update { list ->
            list.map { stream ->
                if (stream.id == streamId) {
                    stream.copy(likeCount = stream.likeCount + 1)
                } else stream
            }
        }
    }

    fun createLiveRoom(
        title: String,
        category: StreamCategory,
        country: Country = POPULAR_COUNTRIES[1],
        isCoinGated: Boolean = false,
        entryCoinPrice: Long = 0,
        snapshotUrl: String = ""
    ): LiveStream {
        val user = _currentUser.value
        val defaultSnapshot = when (category) {
            StreamCategory.MUSIC -> "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800"
            StreamCategory.GAMING -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800"
            StreamCategory.TALENT -> "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800"
            StreamCategory.FITNESS -> "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"
            StreamCategory.CREATIVE -> "https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=800"
            else -> "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800"
        }
        val newStream = LiveStream(
            id = "stream_${System.currentTimeMillis()}",
            hostId = user.id,
            hostName = user.displayName,
            hostAvatar = user.avatarUrl,
            hostRole = UserRole.BROADCASTER,
            title = title.ifBlank { "Live Broadcast with ${user.displayName}" },
            category = category,
            agoraChannelName = "viora_${user.username}_${System.currentTimeMillis() % 10000}",
            viewerCount = 1,
            likeCount = 0,
            diamondCount = 0,
            thumbnailGradientIndex = (0..4).random(),
            snapshotUrl = snapshotUrl.ifBlank { defaultSnapshot },
            countryCode = country.code,
            countryName = country.name,
            countryFlag = country.flagEmoji,
            tags = listOf("live", category.name.lowercase(), country.name.lowercase(), "new"),
            isCoinGated = isCoinGated,
            entryCoinPrice = if (isCoinGated) entryCoinPrice.coerceAtLeast(1L) else 0L,
            unlockedByUserIds = listOf(user.id) // Host always has access
        )
        _liveStreams.update { listOf(newStream) + it }
        _chatMessagesMap.update { map ->
            val initialSystemMessage = listOf(
                ChatMessage(
                    id = "sys_${System.currentTimeMillis()}",
                    streamId = newStream.id,
                    senderId = "sys",
                    senderName = "Viora Guardian",
                    senderAvatar = "",
                    senderRole = UserRole.MODERATOR,
                    content = "Broadcast started. Follow community rules and respect everyone!",
                    type = MessageType.SYSTEM,
                    isModerator = true
                )
            )
            map + (newStream.id to initialSystemMessage)
        }
        return newStream
    }

    fun endLiveRoom(streamId: String) {
        _liveStreams.update { list ->
            list.map { stream ->
                if (stream.id == streamId) stream.copy(status = StreamStatus.ENDED) else stream
            }
        }
    }

    // Chat actions
    fun sendChatMessage(streamId: String, content: String): Boolean {
        val user = _currentUser.value
        if (_mutedUsers.value.contains(user.id)) return false
        if (content.isBlank()) return false

        // Auto-flag: messages with banned keywords are held for moderator review
        val isStaffUser = user.role == UserRole.MODERATOR || user.role == UserRole.ADMIN
        val lowered = content.lowercase()
        val hit = _keywords.value.firstOrNull { lowered.contains(it.lowercase()) }
        if (hit != null && !isStaffUser) {
            _heldMessages.update {
                it + HeldMessage("held_${UUID.randomUUID()}", streamId, user.displayName, content.trim(), hit)
            }
            return true
        }

        val msg = ChatMessage(
            id = "msg_${UUID.randomUUID()}",
            streamId = streamId,
            senderId = user.id,
            senderName = user.displayName,
            senderAvatar = user.avatarUrl,
            senderRole = user.role,
            content = content.trim(),
            type = MessageType.CHAT,
            timestampMs = System.currentTimeMillis(),
            isBroadcaster = user.role == UserRole.BROADCASTER,
            isModerator = user.role == UserRole.MODERATOR || user.role == UserRole.ADMIN,
            userLevel = user.level
        )

        _chatMessagesMap.update { map ->
            val existing = map[streamId] ?: emptyList()
            map + (streamId to (existing + msg))
        }
        return true
    }

    fun pinComment(streamId: String, comment: String) {
        _liveStreams.update { list ->
            list.map { if (it.id == streamId) it.copy(pinnedComment = comment) else it }
        }
    }

    fun deleteChatMessage(streamId: String, messageId: String) {
        if (isStaff()) log("Deleted chat message", messageId)
        _chatMessagesMap.update { map ->
            val list = map[streamId] ?: emptyList()
            map + (streamId to list.filterNot { it.id == messageId })
        }
    }

    // Virtual Gifts
    fun sendGift(streamId: String, gift: VirtualGift, count: Int = 1): Result<GiftTransaction> {
        val totalCost = gift.coinCost * count
        val user = _currentUser.value

        if (_settings.value.giftingPaused) {
            return Result.failure(Exception("Gifting is temporarily paused by Viora"))
        }
        if (count < 1) {
            return Result.failure(Exception("Invalid gift quantity"))
        }

        if (user.coinBalance < totalCost) {
            return Result.failure(Exception("Insufficient coins (Cost: $totalCost, Balance: ${user.coinBalance})"))
        }

        // Deduct coins from sender
        _currentUser.update { it.copy(coinBalance = it.coinBalance - totalCost) }

        // Find recipient stream
        val stream = _liveStreams.value.find { it.id == streamId }
        val recipientName = stream?.hostName ?: "Host"

        // Credit 70% diamond reward to creator
        val diamondsEarned = (totalCost * 0.70).toLong().coerceAtLeast(1L)
        if (stream != null) {
            _liveStreams.update { list ->
                list.map {
                    if (it.id == streamId) it.copy(diamondCount = it.diamondCount + diamondsEarned) else it
                }
            }
        }

        val tx = GiftTransaction(
            id = "gtx_${UUID.randomUUID()}",
            streamId = streamId,
            gift = gift,
            senderName = user.displayName,
            senderAvatar = user.avatarUrl,
            recipientName = recipientName,
            count = count,
            timestampMs = System.currentTimeMillis()
        )

        // Broadcast gift banner
        _activeGiftBanner.value = tx

        // Append gift chat announcement
        val announcement = ChatMessage(
            id = "ann_${UUID.randomUUID()}",
            streamId = streamId,
            senderId = user.id,
            senderName = user.displayName,
            senderAvatar = user.avatarUrl,
            senderRole = user.role,
            content = "Sent ${gift.name} ${gift.emoji} x$count",
            type = MessageType.GIFT_ANNOUNCEMENT,
            timestampMs = System.currentTimeMillis()
        )
        _chatMessagesMap.update { map ->
            val existing = map[streamId] ?: emptyList()
            map + (streamId to (existing + announcement))
        }

        // Add ledger record
        val ledgerTx = LedgerTransaction(
            id = "ltx_${UUID.randomUUID()}",
            type = TransactionType.GIFT_SENT,
            title = "Sent ${gift.name} x$count to $recipientName",
            amountCoins = -totalCost,
            status = TransactionStatus.COMPLETED
        )
        _ledger.update { listOf(ledgerTx) + it }

        return Result.success(tx)
    }

    fun clearGiftBanner() {
        _activeGiftBanner.value = null
    }

    // Wallet actions
    fun buyCoinPackage(pkg: CoinPackage) {
        val totalCoinsAdded = pkg.coins + pkg.bonusCoins
        _currentUser.update { it.copy(coinBalance = it.coinBalance + totalCoinsAdded) }

        val tx = LedgerTransaction(
            id = "ltx_${UUID.randomUUID()}",
            type = TransactionType.COIN_PURCHASE,
            title = "Purchased ${pkg.coins} Coins (+${pkg.bonusCoins} Bonus)",
            amountCoins = totalCoinsAdded,
            amountUsd = pkg.priceUsd,
            status = TransactionStatus.COMPLETED
        )
        _ledger.update { listOf(tx) + it }
        _purchases.update {
            listOf(
                PurchaseRecord(
                    id = "pur_${UUID.randomUUID()}",
                    userName = _currentUser.value.displayName,
                    coins = totalCoinsAdded,
                    usd = pkg.priceUsd.removePrefix("$").toDoubleOrNull() ?: 0.0
                )
            ) + it
        }
    }

    fun requestWithdrawal(diamondsAmount: Long, method: String, destination: String): Result<WithdrawalRequest> {
        val user = _currentUser.value
        if (user.diamondBalance < diamondsAmount) {
            return Result.failure(Exception("Insufficient diamonds"))
        }
        if (diamondsAmount < MIN_WITHDRAWAL_DIAMONDS) {
            return Result.failure(Exception("Minimum cash out is $MIN_WITHDRAWAL_DIAMONDS diamonds ($5.00 USD)"))
        }

        _currentUser.update { it.copy(diamondBalance = it.diamondBalance - diamondsAmount) }
        val usdValue = "$%.2f".format(diamondsAmount / 100.0)
        val accountAgeMs = System.currentTimeMillis() - user.createdAtMs
        val holdUntil = if (accountAgeMs < NEW_CREATOR_HOLD_DAYS * DAY_MS) {
            System.currentTimeMillis() + NEW_CREATOR_HOLD_DAYS * DAY_MS
        } else 0L

        val req = WithdrawalRequest(
            id = "wd_${UUID.randomUUID()}",
            userId = user.id,
            diamondsAmount = diamondsAmount,
            usdValue = usdValue,
            payoutMethod = method,
            payoutDestination = destination,
            status = TransactionStatus.PENDING,
            holdUntilMs = holdUntil
        )
        _withdrawals.update { listOf(req) + it }

        val tx = LedgerTransaction(
            id = "ltx_${UUID.randomUUID()}",
            type = TransactionType.WITHDRAWAL,
            title = "Cash out $usdValue via $method",
            amountDiamonds = -diamondsAmount,
            amountUsd = usdValue,
            status = TransactionStatus.PENDING
        )
        _ledger.update { listOf(tx) + it }

        return Result.success(req)
    }

    // Moderation & Safety actions
    fun muteUser(userId: String) {
        _mutedUsers.update { it + userId }
        if (isStaff()) log("Muted user in live room", userId)
    }

    fun blockUser(userId: String) {
        _blockedUsers.update { it + userId }
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        targetName: String,
        category: ReportCategory,
        notes: String
    ) {
        val report = ModerationReport(
            id = "rep_${UUID.randomUUID()}",
            targetType = targetType,
            targetId = targetId,
            targetName = targetName,
            reportedBy = _currentUser.value.username,
            category = category,
            notes = notes,
            status = ReportStatus.PENDING_REVIEW,
            escalated = category == ReportCategory.UNDERAGE,
            escalationNote = if (category == ReportCategory.UNDERAGE) "Suspected minor: sent straight to admins" else ""
        )
        _reports.update { listOf(report) + it }
    }

    fun resolveReport(reportId: String, action: String) {
        if (isStaff()) log("Report $action", reportId)
        _reports.update { list ->
            list.map { rep ->
                if (rep.id == reportId) {
                    val by = _currentUser.value.displayName
                    val now = System.currentTimeMillis()
                    when (action) {
                        "DISMISS" -> rep.copy(status = ReportStatus.DISMISSED, resolvedByName = by, resolvedMs = now)
                        "ACTION_TAKEN" -> rep.copy(status = ReportStatus.ACTION_TAKEN, resolvedByName = by, resolvedMs = now)
                        else -> rep
                    }
                } else rep
            }
        }
    }

    fun adminTerminateStream(streamId: String) {
        if (!isAdmin()) return
        log("Killed live room", streamId)
        endLiveRoom(streamId)
        _reports.update { list ->
            list.map { if (it.targetId == streamId) it.copy(status = ReportStatus.ACTION_TAKEN) else it }
        }
    }

    // Creator Profile Media & Coin Unlocks
    fun uploadProfileMedia(
        mediaType: MediaType,
        mediaUrl: String,
        caption: String,
        isLocked: Boolean,
        coinPrice: Long
    ) {
        val user = _currentUser.value
        val newMedia = ProfileMedia(
            id = "media_${System.currentTimeMillis()}",
            userId = user.id,
            authorName = user.displayName,
            authorAvatar = user.avatarUrl,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            thumbnailUrl = mediaUrl,
            caption = caption,
            isLocked = isLocked,
            coinPrice = if (isLocked) coinPrice.coerceAtLeast(1L) else 0L,
            likesCount = 0,
            unlockedByUserIds = listOf(user.id)
        )
        _profileMedia.update { listOf(newMedia) + it }
    }

    fun unlockProfileMedia(mediaId: String): Result<ProfileMedia> {
        val user = _currentUser.value
        val media = _profileMedia.value.find { it.id == mediaId }
            ?: return Result.failure(Exception("Media not found"))

        if (media.unlockedByUserIds.contains(user.id) || media.userId == user.id || !media.isLocked) {
            return Result.success(media)
        }

        if (user.coinBalance < media.coinPrice) {
            return Result.failure(Exception("Insufficient coins (Required: ${media.coinPrice}, Balance: ${user.coinBalance})"))
        }

        // Deduct coins from user
        _currentUser.update { it.copy(coinBalance = it.coinBalance - media.coinPrice) }

        // 70% Diamonds credited to creator
        val diamondsEarned = (media.coinPrice * 0.70).toLong().coerceAtLeast(1L)
        if (media.userId == user.id) {
            _currentUser.update { it.copy(diamondBalance = it.diamondBalance + diamondsEarned) }
        }

        // Mark media unlocked for this viewer
        val updatedMedia = media.copy(unlockedByUserIds = media.unlockedByUserIds + user.id)
        _profileMedia.update { list ->
            list.map { if (it.id == mediaId) updatedMedia else it }
        }

        // Record financial ledger transaction
        val tx = LedgerTransaction(
            id = "ltx_${UUID.randomUUID()}",
            type = TransactionType.GIFT_SENT,
            title = "Unlocked Exclusive Media (${media.caption.take(24)})",
            amountCoins = -media.coinPrice,
            status = TransactionStatus.COMPLETED
        )
        _ledger.update { listOf(tx) + it }

        return Result.success(updatedMedia)
    }

    fun unlockLiveStream(streamId: String): Result<LiveStream> {
        val user = _currentUser.value
        val stream = _liveStreams.value.find { it.id == streamId }
            ?: return Result.failure(Exception("Stream not found"))

        if (stream.unlockedByUserIds.contains(user.id) || stream.hostId == user.id || !stream.isCoinGated) {
            return Result.success(stream)
        }

        if (user.coinBalance < stream.entryCoinPrice) {
            return Result.failure(Exception("Insufficient coins (Entry fee: ${stream.entryCoinPrice}, Balance: ${user.coinBalance})"))
        }

        // Deduct entry coins
        _currentUser.update { it.copy(coinBalance = it.coinBalance - stream.entryCoinPrice) }

        // Credit 70% Diamonds to broadcaster
        val diamondsEarned = (stream.entryCoinPrice * 0.70).toLong().coerceAtLeast(1L)
        val updatedStream = stream.copy(
            diamondCount = stream.diamondCount + diamondsEarned,
            unlockedByUserIds = stream.unlockedByUserIds + user.id
        )

        _liveStreams.update { list ->
            list.map { if (it.id == streamId) updatedStream else it }
        }

        // Ledger record
        val tx = LedgerTransaction(
            id = "ltx_${UUID.randomUUID()}",
            type = TransactionType.GIFT_SENT,
            title = "VIP Stream Ticket: ${stream.title.take(24)}",
            amountCoins = -stream.entryCoinPrice,
            status = TransactionStatus.COMPLETED
        )
        _ledger.update { listOf(tx) + it }

        return Result.success(updatedStream)
    }

    // =====================================================================
    // OPERATIONS: roles, tasks, strikes, appeals, money controls, kill switch
    // Demo logic only. Real enforcement lives in Supabase (see migration
    // 20261029000000_roles_and_safety.sql); never trust the client.
    // =====================================================================
    private const val DAY_MS = 24L * 3600 * 1000

    private fun isStaff() = _currentUser.value.role == UserRole.MODERATOR || _currentUser.value.role == UserRole.ADMIN
    private fun isAdmin() = _currentUser.value.role == UserRole.ADMIN

    private val _audit = MutableStateFlow<List<AuditEntry>>(emptyList())
    val auditLog: StateFlow<List<AuditEntry>> = _audit.asStateFlow()

    private fun log(action: String, target: String) {
        val u = _currentUser.value
        _audit.update {
            listOf(AuditEntry("aud_${UUID.randomUUID()}", u.displayName, u.role, action, target)) + it
        }
    }

    private val _moderators = MutableStateFlow(
        listOf(
            ModeratorInfo("usr_mod_1", "Marcus", true),
            ModeratorInfo("usr_mod_2", "Sara", true),
            ModeratorInfo("usr_mod_3", "Dayo", false)
        )
    )
    val moderators: StateFlow<List<ModeratorInfo>> = _moderators.asStateFlow()

    private val _tasks = MutableStateFlow(
        listOf(
            ModerationTask("task_1", "Review spam report", "Check stream promo link in description", "rep_01",
                "usr_me_01", "Alex Rivers", "Admin Team", TaskPriority.URGENT),
            ModerationTask("task_2", "Review harassment report", "Check chat history of troll_account_09", "rep_02",
                "usr_mod_1", "Marcus", "Admin Team", TaskPriority.NORMAL, TaskStatus.IN_PROGRESS)
        )
    )
    val tasks: StateFlow<List<ModerationTask>> = _tasks.asStateFlow()

    private val _platformUsers = MutableStateFlow(
        listOf(
            PlatformUser("usr_02", "Chloe Beats", UserRole.BROADCASTER, 0, AccountStatus.ACTIVE, 3200, 0),
            PlatformUser("usr_03", "Neon K", UserRole.BROADCASTER, 0, AccountStatus.ACTIVE, 900, 0),
            PlatformUser("usr_04", "Maya Lin", UserRole.BROADCASTER, 1, AccountStatus.ACTIVE, 150, 1),
            PlatformUser("usr_05", "Sam Steele", UserRole.BROADCASTER, 0, AccountStatus.ACTIVE, 60, 0),
            PlatformUser("usr_99", "troll_account_09", UserRole.VIEWER, 4, AccountStatus.BANNED, 0, 3),
            PlatformUser("usr_mod_1", "Marcus", UserRole.MODERATOR, 0, AccountStatus.ACTIVE, 0, 0),
            PlatformUser("usr_mod_2", "Sara", UserRole.MODERATOR, 0, AccountStatus.ACTIVE, 0, 0),
            PlatformUser("usr_mod_3", "Dayo", UserRole.MODERATOR, 0, AccountStatus.ACTIVE, 0, 0)
        )
    )
    val platformUsers: StateFlow<List<PlatformUser>> = _platformUsers.asStateFlow()

    private val _appeals = MutableStateFlow(
        listOf(Appeal("apl_1", "usr_99", "troll_account_09", "My account was used by my little brother. Please give me another chance."))
    )
    val appeals: StateFlow<List<Appeal>> = _appeals.asStateFlow()

    private val _purchases = MutableStateFlow(
        listOf(
            PurchaseRecord("pur_1", "Sara Croft", 6500 + 1200, 49.99, PurchaseStatus.COMPLETED, System.currentTimeMillis() - 3600000),
            PurchaseRecord("pur_2", "Brian O.", 1200 + 150, 9.99, PurchaseStatus.COMPLETED, System.currentTimeMillis() - 7200000),
            PurchaseRecord("pur_3", "new_user_771", 15000 + 3500, 99.99, PurchaseStatus.FLAGGED, System.currentTimeMillis() - 9000000),
            PurchaseRecord("pur_4", "Lina M.", 500 + 50, 4.99, PurchaseStatus.COMPLETED, System.currentTimeMillis() - 86400000)
        )
    )
    val purchases: StateFlow<List<PurchaseRecord>> = _purchases.asStateFlow()

    private val _fraudAlerts = MutableStateFlow(
        listOf(
            FraudAlert("fr_1", "new_user_771", "New account bought 18,500 coins and gifted all of it within 10 minutes", "HIGH"),
            FraudAlert("fr_2", "gifter_42", "100% of gifts went to a single host (Neon K) across 9 purchases", "MEDIUM"),
            FraudAlert("fr_3", "vip_charge_8", "3 purchase refunds requested in 24 hours", "MEDIUM")
        )
    )
    val fraudAlerts: StateFlow<List<FraudAlert>> = _fraudAlerts.asStateFlow()

    private val _applications = MutableStateFlow(
        listOf(
            CreatorApplication("app_1", "usr_201", "Zawadi K", "I stream cooking shows and Swahili music 3x a week."),
            CreatorApplication("app_2", "usr_202", "Tito B", "Gaming streamer, 12k followers on other platforms.")
        )
    )
    val applications: StateFlow<List<CreatorApplication>> = _applications.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _heldMessages = MutableStateFlow<List<HeldMessage>>(emptyList())
    val heldMessages: StateFlow<List<HeldMessage>> = _heldMessages.asStateFlow()

    private val _keywords = MutableStateFlow(DEFAULT_BANNED_KEYWORDS)
    val keywords: StateFlow<List<String>> = _keywords.asStateFlow()

    private val _settings = MutableStateFlow(PlatformSettings())
    val settings: StateFlow<PlatformSettings> = _settings.asStateFlow()

    private val _featured = MutableStateFlow<Set<String>>(emptySet())
    val featuredStreamIds: StateFlow<Set<String>> = _featured.asStateFlow()

    private val _otherWithdrawals = MutableStateFlow(
        listOf(
            WithdrawalRequest("wd_s1", "usr_02", 2000, "$20.00", "PayPal", "chloe@example.com"),
            WithdrawalRequest("wd_s2", "usr_03", 60000, "$600.00", "M-Pesa", "+254700000000")
        )
    )
    val otherWithdrawals: StateFlow<List<WithdrawalRequest>> = _otherWithdrawals.asStateFlow()

    // ---- Tasks ----
    fun assignTask(title: String, details: String, reportId: String?, moderatorId: String, priority: TaskPriority) {
        if (!isAdmin() || title.isBlank()) return
        val mod = _moderators.value.find { it.id == moderatorId } ?: return
        val task = ModerationTask(
            id = "task_${UUID.randomUUID()}", title = title.trim(), details = details.trim(), reportId = reportId,
            assignedToId = mod.id, assignedToName = mod.name, assignedByName = _currentUser.value.displayName,
            priority = priority
        )
        _tasks.update { listOf(task) + it }
        if (reportId != null) {
            _reports.update { list -> list.map { if (it.id == reportId) it.copy(assignedToName = mod.name, assignedToId = mod.id) else it } }
        }
        log("Assigned task to ${mod.name}", title)
    }

    fun updateTask(taskId: String, status: TaskStatus, note: String) {
        if (!isStaff()) return
        _tasks.update { list -> list.map { if (it.id == taskId) it.copy(status = status, note = note.ifBlank { it.note }) else it } }
        log("Task marked ${status.name}", taskId)
    }

    // ---- Strikes (warning -> 24h mute -> suspension -> ban) ----
    fun applyStrike(userName: String, reason: String) {
        if (!isStaff()) return
        _platformUsers.update { list ->
            val existing = list.find { it.name.equals(userName, ignoreCase = true) }
            if (existing == null) {
                list + PlatformUser("usr_${UUID.randomUUID()}", userName, strikes = 1)
            } else {
                list.map {
                    if (it.id == existing.id) {
                        val n = it.strikes + 1
                        val status = when {
                            n >= 4 -> AccountStatus.BANNED
                            n == 3 -> AccountStatus.SUSPENDED
                            n == 2 -> AccountStatus.MUTED
                            else -> AccountStatus.ACTIVE
                        }
                        it.copy(strikes = n, status = status)
                    } else it
                }
            }
        }
        log("Strike issued: $reason", userName)
    }

    fun warnUser(userName: String, message: String, reportId: String?) {
        applyStrike(userName, message)
        if (reportId != null) resolveReport(reportId, "ACTION_TAKEN")
    }

    fun escalateReport(reportId: String, note: String) {
        if (!isStaff()) return
        _reports.update { list -> list.map { if (it.id == reportId) it.copy(escalated = true, escalationNote = note) else it } }
        log("Escalated report to admin", reportId)
    }

    fun hideReportedMedia(mediaId: String, reportId: String?) {
        if (!isStaff()) return
        _profileMedia.update { list -> list.filterNot { it.id == mediaId } }
        log("Hid reported media", mediaId)
        if (reportId != null) resolveReport(reportId, "ACTION_TAKEN")
    }

    // ---- Users and roles (admin) ----
    fun setUserRole(userId: String, role: UserRole) {
        if (!isAdmin()) return
        _platformUsers.update { list -> list.map { if (it.id == userId) it.copy(role = role) else it } }
        log("Changed role to ${role.name}", userId)
    }

    fun banUser(userId: String) {
        if (!isAdmin()) return
        _platformUsers.update { list -> list.map { if (it.id == userId) it.copy(status = AccountStatus.BANNED, strikes = maxOf(it.strikes, 4)) else it } }
        log("Banned user", userId)
    }

    fun unbanUser(userId: String) {
        if (!isAdmin()) return
        _platformUsers.update { list -> list.map { if (it.id == userId) it.copy(status = AccountStatus.ACTIVE, strikes = 0) else it } }
        log("Unbanned user", userId)
    }

    // ---- Appeals ----
    fun submitAppeal(reason: String) {
        if (reason.isBlank()) return
        val u = _currentUser.value
        _appeals.update { listOf(Appeal("apl_${UUID.randomUUID()}", u.id, u.displayName, reason.trim())) + it }
        if (SupabaseApi.isConfigured) {
            repoScope.launch {
                runCatching { SupabaseApi.insert("appeals", JSONObject().put("user_id", u.id).put("reason", reason.trim())) }
                runCatching { loadMyAppeals() }
            }
        }
    }

    fun decideAppeal(appealId: String, approve: Boolean) {
        if (!isAdmin()) return
        val appeal = _appeals.value.find { it.id == appealId } ?: return
        _appeals.update { list ->
            list.map { if (it.id == appealId) it.copy(status = if (approve) AppealStatus.APPROVED else AppealStatus.REJECTED) else it }
        }
        if (approve) unbanUser(appeal.userId)
        log(if (approve) "Approved appeal" else "Rejected appeal", appeal.userName)
    }

    // ---- Money: purchases, fraud, withdrawals ----
    fun refundPurchase(id: String) {
        if (!isAdmin()) return
        _purchases.update { list -> list.map { if (it.id == id) it.copy(status = PurchaseStatus.REFUNDED) else it } }
        log("Refunded purchase", id)
    }

    fun flagPurchase(id: String) {
        if (!isAdmin()) return
        _purchases.update { list -> list.map { if (it.id == id) it.copy(status = PurchaseStatus.FLAGGED) else it } }
        log("Flagged purchase as fraud", id)
    }

    fun resolveFraudAlert(id: String, ban: Boolean) {
        if (!isAdmin()) return
        val alert = _fraudAlerts.value.find { it.id == id } ?: return
        _fraudAlerts.update { list -> list.map { if (it.id == id) it.copy(resolved = true) else it } }
        if (ban) {
            _platformUsers.value.find { it.name.equals(alert.userName, true) }?.let { banUser(it.id) }
        }
        log(if (ban) "Fraud alert resolved with ban" else "Fraud alert dismissed", alert.userName)
    }

    private fun mapWithdrawal(id: String, fn: (WithdrawalRequest) -> WithdrawalRequest) {
        _withdrawals.update { list -> list.map { if (it.id == id) fn(it) else it } }
        _otherWithdrawals.update { list -> list.map { if (it.id == id) fn(it) else it } }
    }

    /** Returns an error message, or null when the approval was recorded. */
    fun approveWithdrawal(id: String): String? {
        if (!isAdmin()) return "Admins only"
        val req = (_withdrawals.value + _otherWithdrawals.value).find { it.id == id } ?: return "Not found"
        val me = _currentUser.value.id
        if (System.currentTimeMillis() < req.holdUntilMs) return "New-creator security hold is still active"
        if (me in req.approvals) return "You already approved this request. A second admin must confirm."
        val approvals = req.approvals + me
        val needed = if (req.diamondsAmount >= LARGE_WITHDRAWAL_DIAMONDS) 2 else 1
        mapWithdrawal(id) {
            it.copy(approvals = approvals, status = if (approvals.size >= needed) TransactionStatus.COMPLETED else TransactionStatus.PENDING)
        }
        log(if (approvals.size >= needed) "Approved withdrawal (paid)" else "First approval on large withdrawal", id)
        return null
    }

    fun rejectWithdrawal(id: String) {
        if (!isAdmin()) return
        mapWithdrawal(id) { it.copy(status = TransactionStatus.REJECTED) }
        log("Rejected withdrawal", id)
    }

    // ---- Creator applications ----
    fun applyForCreator(pitch: String) {
        if (pitch.isBlank()) return
        val u = _currentUser.value
        _applications.update { listOf(CreatorApplication("app_${UUID.randomUUID()}", u.id, u.displayName, pitch.trim())) + it }
        if (SupabaseApi.isConfigured) {
            repoScope.launch {
                runCatching { SupabaseApi.insert("creator_applications", JSONObject().put("user_id", u.id).put("pitch", pitch.trim())) }
            }
        }
    }

    fun decideApplication(appId: String, approve: Boolean) {
        if (!isAdmin()) return
        val app = _applications.value.find { it.id == appId } ?: return
        _applications.update { list ->
            list.map { if (it.id == appId) it.copy(status = if (approve) ApplicationStatus.APPROVED else ApplicationStatus.REJECTED) else it }
        }
        if (approve) {
            if (app.userId == _currentUser.value.id) {
                _currentUser.update { it.copy(role = UserRole.BROADCASTER, isVerified = true) }
            }
            _platformUsers.update { list -> list.map { if (it.id == app.userId) it.copy(role = UserRole.BROADCASTER) else it } }
        }
        log(if (approve) "Approved creator" else "Rejected creator", app.userName)
    }

    // ---- Announcements, featured, keywords, kill switch ----
    fun postAnnouncement(title: String, body: String) {
        if (!isAdmin() || title.isBlank()) return
        _announcements.update { listOf(Announcement("ann_${UUID.randomUUID()}", title.trim(), body.trim())) + it }
        log("Posted announcement", title)
    }

    fun toggleFeatured(streamId: String) {
        if (!isAdmin()) return
        _featured.update { if (streamId in it) it - streamId else it + streamId }
        log("Toggled featured stream", streamId)
    }

    fun addKeyword(word: String) {
        if (!isAdmin() || word.isBlank()) return
        _keywords.update { if (it.any { k -> k.equals(word.trim(), true) }) it else it + word.trim().lowercase() }
        log("Added banned keyword", word.trim())
    }

    fun removeKeyword(word: String) {
        if (!isAdmin()) return
        _keywords.update { it - word }
        log("Removed banned keyword", word)
    }

    fun decideHeldMessage(id: String, release: Boolean) {
        if (!isStaff()) return
        val held = _heldMessages.value.find { it.id == id } ?: return
        _heldMessages.update { list -> list.filterNot { it.id == id } }
        if (release) {
            val msg = ChatMessage(
                id = "msg_${UUID.randomUUID()}", streamId = held.streamId, senderId = "held",
                senderName = held.senderName, senderAvatar = "", senderRole = UserRole.VIEWER,
                content = held.content, type = MessageType.CHAT, timestampMs = System.currentTimeMillis(),
                isBroadcaster = false, isModerator = false, userLevel = 1
            )
            _chatMessagesMap.update { map -> map + (held.streamId to ((map[held.streamId] ?: emptyList()) + msg)) }
        } else {
            applyStrike(held.senderName, "Blocked keyword: ${held.matchedKeyword}")
        }
        log(if (release) "Released held chat message" else "Rejected held chat message", held.senderName)
    }

    fun setGiftingPaused(paused: Boolean) {
        if (!isAdmin()) return
        _settings.update { it.copy(giftingPaused = paused) }
        log(if (paused) "KILL SWITCH: gifting paused" else "Gifting resumed", "platform")
    }

    fun setStreamsPaused(paused: Boolean) {
        if (!isAdmin()) return
        _settings.update { it.copy(streamsPaused = paused) }
        if (paused) {
            _liveStreams.update { list -> list.map { if (it.status == StreamStatus.LIVE) it.copy(status = StreamStatus.ENDED) else it } }
        }
        log(if (paused) "KILL SWITCH: all live streams paused" else "Live streaming resumed", "platform")
    }

    // ---- Account deletion (required by Google Play) ----
    suspend fun deleteAccount(): Result<Unit> {
        if (SupabaseApi.isConfigured) {
            val result = runCatching { SupabaseApi.rpc("delete_my_account") }
            if (result.isFailure) return Result.failure(result.exceptionOrNull() ?: Exception("Could not delete account"))
            SupabaseApi.signOut()
        } else {
            log("Account deleted by user", _currentUser.value.id)
            _currentUser.value = MockDataProvider.currentUser
        }
        _isAuthenticated.value = false
        return Result.success(Unit)
    }

    // ---- Debug-only preview (does nothing in release builds) ----
    fun debugPreviewRole(role: UserRole) {
        if (!BuildConfig.DEBUG) return
        _currentUser.update { it.copy(role = role) }
    }

    fun debugPreviewBanned(banned: Boolean) {
        if (!BuildConfig.DEBUG) return
        _currentUser.update { it.copy(isBanned = banned) }
    }

    // ---- Catalog (admin edits coin packages and gifts) ----
    private val _coinPackages = MutableStateFlow(COIN_PACKAGES)
    val coinPackages: StateFlow<List<CoinPackage>> = _coinPackages.asStateFlow()

    private val _giftCatalog = MutableStateFlow(DEFAULT_GIFTS)
    val giftCatalog: StateFlow<List<VirtualGift>> = _giftCatalog.asStateFlow()

    fun updateCoinPackage(id: String, coins: Long, bonus: Long, priceUsd: String) {
        if (!isAdmin() || coins <= 0 || bonus < 0 || priceUsd.isBlank()) return
        _coinPackages.update { list -> list.map { if (it.id == id) it.copy(coins = coins, bonusCoins = bonus, priceUsd = priceUsd.trim()) else it } }
        log("Edited coin package", id)
    }

    fun setCoinPackageActive(id: String, active: Boolean) {
        if (!isAdmin()) return
        _coinPackages.update { list -> list.map { if (it.id == id) it.copy(isActive = active) else it } }
        log(if (active) "Enabled coin package" else "Disabled coin package", id)
    }

    fun updateGiftCost(id: String, coinCost: Long) {
        if (!isAdmin() || coinCost <= 0) return
        _giftCatalog.update { list -> list.map { if (it.id == id) it.copy(coinCost = coinCost) else it } }
        log("Changed gift price", id)
    }

    fun setGiftActive(id: String, active: Boolean) {
        if (!isAdmin()) return
        _giftCatalog.update { list -> list.map { if (it.id == id) it.copy(isActive = active) else it } }
        log(if (active) "Enabled gift" else "Disabled gift", id)
    }

    // ---- Moderator claims an unassigned report ----
    fun claimReport(reportId: String) {
        if (!isStaff()) return
        val me = _currentUser.value
        _reports.update { list ->
            list.map { if (it.id == reportId && it.assignedToId.isBlank()) it.copy(assignedToId = me.id, assignedToName = me.displayName) else it }
        }
        log("Claimed report", reportId)
    }
}
