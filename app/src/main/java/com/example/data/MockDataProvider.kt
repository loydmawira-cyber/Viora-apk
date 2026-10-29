package com.example.data

import com.example.model.*

object MockDataProvider {

    val currentUser = User(
        id = "usr_me_01",
        username = "alex_viora",
        displayName = "Alex Rivers",
        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
        bio = "Exploring sound & vibes ✨ Catch me live every Tue/Fri!",
        role = UserRole.VIEWER,
        coinBalance = 2450,
        diamondBalance = 840,
        followersCount = 3890,
        followingCount = 184,
        isVerified = true,
        level = 18,
        createdAtMs = System.currentTimeMillis() - 90L * 24 * 3600 * 1000
    )

    val sampleUsers = listOf(
        User("usr_04", "maya_dance", "Maya Lin", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", "Choreographer & dancer 💃", UserRole.BROADCASTER, 450, 4900, 58200, 310, true, 31),
        User("usr_06", "elena_art", "Elena Canvas", "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150", "Digital art & 3D sculpting 🎨", UserRole.BROADCASTER, 600, 920, 8900, 112, true, 16),
        User("usr_05", "sam_fitness", "Sam Steele", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", "Daily HIIT & motivation 🔥", UserRole.BROADCASTER, 900, 1200, 14500, 60, false, 14),
        User("usr_02", "chloe_tunes", "Chloe Beats", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150", "Singer-songwriter & loop artist 🎸", UserRole.BROADCASTER, 1200, 3400, 42100, 142, true, 24),
        User("usr_03", "neon_rider", "Neon K", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150", "Cyberpunk game speedruns 🎮", UserRole.BROADCASTER, 800, 1850, 19400, 95, true, 19),
        User("usr_07", "marcus_mod", "Marcus Vance", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150", "Community guardian 🛡️", UserRole.MODERATOR, 1500, 100, 3200, 450, false, 20)
    )

    val initialStreams = listOf(
        LiveStream(
            id = "stream_03",
            hostId = "usr_04",
            hostName = "Maya Lin",
            hostAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
            title = "K-Pop Choreography Workshop & Q&A 💃✨",
            category = StreamCategory.TALENT,
            agoraChannelName = "viora_talent_maya",
            viewerCount = 940,
            likeCount = 6120,
            diamondCount = 2100,
            thumbnailGradientIndex = 2,
            snapshotUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
            countryCode = "KR",
            countryName = "South Korea",
            countryFlag = "🇰🇷",
            isFollowing = false,
            tags = listOf("dance", "kpop", "tutorial", "workshop")
        ),
        LiveStream(
            id = "stream_05",
            hostId = "usr_06",
            hostName = "Elena Canvas",
            hostAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150",
            title = "Sculpting Cyberpunk Creatures in Blender 3D 🎨",
            category = StreamCategory.CREATIVE,
            agoraChannelName = "viora_art_elena",
            viewerCount = 620,
            likeCount = 4500,
            diamondCount = 1420,
            thumbnailGradientIndex = 4,
            snapshotUrl = "https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=800",
            countryCode = "FR",
            countryName = "France",
            countryFlag = "🇫🇷",
            isFollowing = true,
            tags = listOf("blender", "3dart", "creative", "cgi")
        ),
        LiveStream(
            id = "stream_04",
            hostId = "usr_05",
            hostName = "Sam Steele",
            hostAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
            title = "Evening 30-Min Core Shred & Nutrition Chat 🏋️‍♂️",
            category = StreamCategory.FITNESS,
            agoraChannelName = "viora_fit_sam",
            viewerCount = 480,
            likeCount = 3200,
            diamondCount = 890,
            thumbnailGradientIndex = 3,
            snapshotUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
            countryCode = "GB",
            countryName = "United Kingdom",
            countryFlag = "🇬🇧",
            isFollowing = false,
            tags = listOf("fitness", "hiit", "workout", "health")
        ),
        LiveStream(
            id = "stream_01",
            hostId = "usr_02",
            hostName = "Chloe Beats",
            hostAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
            title = "Acoustic Sunset Jam & Song Requests 🎸✨",
            category = StreamCategory.MUSIC,
            agoraChannelName = "viora_music_chloe",
            viewerCount = 1482,
            likeCount = 8940,
            diamondCount = 3400,
            thumbnailGradientIndex = 0,
            snapshotUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800",
            countryCode = "US",
            countryName = "United States",
            countryFlag = "🇺🇸",
            pinnedComment = "Drop your favorite 90s indie requests in chat! 🎶",
            isFollowing = true,
            tags = listOf("acoustic", "guitar", "livejam", "sunset")
        ),
        LiveStream(
            id = "stream_02",
            hostId = "usr_03",
            hostName = "Neon K",
            hostAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
            title = "Night City Speedrun World Record Attempt 🕹️🚀",
            category = StreamCategory.GAMING,
            agoraChannelName = "viora_gaming_neon",
            viewerCount = 3120,
            likeCount = 21400,
            diamondCount = 7650,
            thumbnailGradientIndex = 1,
            snapshotUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800",
            countryCode = "JP",
            countryName = "Japan",
            countryFlag = "🇯🇵",
            pinnedComment = "Sub 45min split pace right now! Hyped!!",
            isFollowing = true,
            tags = listOf("speedrun", "cyberpunk", "gaming", "pc"),
            isCoinGated = true,
            entryCoinPrice = 30
        )
    )

    fun sampleChatMessages(streamId: String): List<ChatMessage> {
        val now = System.currentTimeMillis()
        return listOf(
            ChatMessage("msg_1", streamId, "usr_07", "Marcus Vance", "", UserRole.MODERATOR, "Welcome everyone to Viora! Keep chat respectful and kind ❤️", MessageType.SYSTEM, now - 60000, false, true, 20),
            ChatMessage("msg_2", streamId, "usr_10", "Jordan Lee", "", UserRole.VIEWER, "That intro riff was incredible!! 🔥", MessageType.CHAT, now - 45000, false, false, 8),
            ChatMessage("msg_3", streamId, "usr_11", "Sara Croft", "", UserRole.VIEWER, "Sending roses! 🌹🌹🌹", MessageType.CHAT, now - 35000, false, false, 12),
            ChatMessage("msg_4", streamId, "usr_02", "Chloe Beats", "", UserRole.BROADCASTER, "Thank you Sara! Playing your request next! ✨", MessageType.CHAT, now - 25000, true, false, 24),
            ChatMessage("msg_5", streamId, "usr_12", "David Kim", "", UserRole.VIEWER, "The audio quality is so crisp today 🎧", MessageType.CHAT, now - 15000, false, false, 4),
            ChatMessage("msg_6", streamId, "usr_13", "Mia Rivera", "", UserRole.VIEWER, "Let's hit 10k likes team! 💖", MessageType.CHAT, now - 5000, false, false, 15)
        )
    }

    val initialLedger = listOf(
        LedgerTransaction("tx_01", TransactionType.COIN_PURCHASE, "Purchased 1,200 Coins (+150 Bonus)", 1350, 0, "$9.99", TransactionStatus.COMPLETED, System.currentTimeMillis() - 86400000),
        LedgerTransaction("tx_02", TransactionType.GIFT_SENT, "Sent Sports Car to Chloe Beats", -200, 0, "", TransactionStatus.COMPLETED, System.currentTimeMillis() - 72000000),
        LedgerTransaction("tx_03", TransactionType.GIFT_SENT, "Sent Sparkle Heart x5 to Neon K", -50, 0, "", TransactionStatus.COMPLETED, System.currentTimeMillis() - 36000000),
        LedgerTransaction("tx_04", TransactionType.GIFT_RECEIVED, "Received Galactic Rocket from Sara Croft", 0, 350, "", TransactionStatus.COMPLETED, System.currentTimeMillis() - 18000000)
    )

    val initialReports = listOf(
        ModerationReport("rep_01", "STREAM", "stream_03", "Maya Lin", "anonymous_user", ReportCategory.SPAM, "Repeated identical commercial promo link in description", ReportStatus.PENDING_REVIEW, System.currentTimeMillis() - 1200000),
        ModerationReport("rep_02", "USER", "usr_99", "troll_account_09", "marcus_mod", ReportCategory.HARASSMENT, "Targeted insult attacks in chat comments", ReportStatus.PENDING_REVIEW, System.currentTimeMillis() - 3600000)
    )

    val initialProfileMedia = listOf(
        ProfileMedia(
            id = "media_01",
            userId = "usr_me_01",
            authorName = "Alex Rivers",
            authorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            mediaType = MediaType.PHOTO,
            mediaUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
            thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
            caption = "Studio sound check before tonight's broadcast 🎙️✨",
            isLocked = false,
            coinPrice = 0,
            likesCount = 384
        )
    )
}
