package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

object VioraRepository {

    private val _currentUser = MutableStateFlow(MockDataProvider.currentUser)
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(true)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

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

    private val _reports = MutableStateFlow(MockDataProvider.initialReports)
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

    fun logout() {
        _isAuthenticated.value = false
    }

    fun switchRole(role: UserRole) {
        _currentUser.update { it.copy(role = role) }
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
        _chatMessagesMap.update { map ->
            val list = map[streamId] ?: emptyList()
            map + (streamId to list.filterNot { it.id == messageId })
        }
    }

    // Virtual Gifts
    fun sendGift(streamId: String, gift: VirtualGift, count: Int = 1): Result<GiftTransaction> {
        val totalCost = gift.coinCost * count
        val user = _currentUser.value

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
    }

    fun topUpTestCoins(amount: Long = 1000) {
        _currentUser.update { it.copy(coinBalance = it.coinBalance + amount) }
        val tx = LedgerTransaction(
            id = "ltx_${UUID.randomUUID()}",
            type = TransactionType.COIN_PURCHASE,
            title = "Dev Sandbox Top-up ($amount Coins)",
            amountCoins = amount,
            amountUsd = "$0.00",
            status = TransactionStatus.COMPLETED
        )
        _ledger.update { listOf(tx) + it }
    }

    fun requestWithdrawal(diamondsAmount: Long, method: String, destination: String): Result<WithdrawalRequest> {
        val user = _currentUser.value
        if (user.diamondBalance < diamondsAmount) {
            return Result.failure(Exception("Insufficient diamonds"))
        }
        if (diamondsAmount < 100) {
            return Result.failure(Exception("Minimum cash out is 100 diamonds ($1.00 USD)"))
        }

        _currentUser.update { it.copy(diamondBalance = it.diamondBalance - diamondsAmount) }
        val usdValue = "$%.2f".format(diamondsAmount / 100.0)

        val req = WithdrawalRequest(
            id = "wd_${UUID.randomUUID()}",
            userId = user.id,
            diamondsAmount = diamondsAmount,
            usdValue = usdValue,
            payoutMethod = method,
            payoutDestination = destination,
            status = TransactionStatus.PENDING
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
            status = ReportStatus.PENDING_REVIEW
        )
        _reports.update { listOf(report) + it }
    }

    fun resolveReport(reportId: String, action: String) {
        _reports.update { list ->
            list.map { rep ->
                if (rep.id == reportId) {
                    when (action) {
                        "DISMISS" -> rep.copy(status = ReportStatus.DISMISSED)
                        "ACTION_TAKEN" -> rep.copy(status = ReportStatus.ACTION_TAKEN)
                        else -> rep
                    }
                } else rep
            }
        }
    }

    fun adminTerminateStream(streamId: String) {
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
}
