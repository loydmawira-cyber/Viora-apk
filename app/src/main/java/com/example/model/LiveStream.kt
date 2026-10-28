package com.example.model

enum class StreamCategory(val title: String, val iconName: String) {
    ALL("All", "Sparkles"),
    TRENDING("Trending", "Whatshot"),
    MUSIC("Music", "MusicNote"),
    GAMING("Gaming", "SportsEsports"),
    TALENT("Talent", "Star"),
    CHATTING("Just Chatting", "Chat"),
    FITNESS("Fitness", "FitnessCenter"),
    CREATIVE("Art & Design", "Brush")
}

enum class StreamStatus {
    LIVE,
    PAUSED,
    ENDED
}

enum class StreamSortOrder(val title: String) {
    POPULAR("Most Viewers"),
    TRENDING("Most Likes"),
    NEWEST("Recently Started"),
    DIAMONDS("Top Diamonds")
}

data class Country(
    val code: String,
    val name: String,
    val flagEmoji: String
)

val POPULAR_COUNTRIES = listOf(
    Country("ALL", "Global", "🌍"),
    Country("US", "United States", "🇺🇸"),
    Country("GB", "United Kingdom", "🇬🇧"),
    Country("JP", "Japan", "🇯🇵"),
    Country("KR", "South Korea", "🇰🇷"),
    Country("BR", "Brazil", "🇧🇷"),
    Country("KE", "Kenya", "🇰🇪"),
    Country("FR", "France", "🇫🇷"),
    Country("DE", "Germany", "🇩🇪"),
    Country("IN", "India", "🇮🇳"),
    Country("CA", "Canada", "🇨🇦"),
    Country("NG", "Nigeria", "🇳🇬")
)

data class LiveStream(
    val id: String,
    val hostId: String,
    val hostName: String,
    val hostAvatar: String,
    val hostRole: UserRole = UserRole.BROADCASTER,
    val title: String,
    val category: StreamCategory,
    val agoraChannelName: String,
    val status: StreamStatus = StreamStatus.LIVE,
    val viewerCount: Int = 120,
    val likeCount: Int = 450,
    val diamondCount: Long = 850,
    val startedAtEpochMs: Long = System.currentTimeMillis() - (15 * 60 * 1000),
    val thumbnailGradientIndex: Int = 0,
    val snapshotUrl: String = "",
    val countryCode: String = "US",
    val countryName: String = "United States",
    val countryFlag: String = "🇺🇸",
    val pinnedComment: String? = null,
    val isFollowing: Boolean = false,
    val tags: List<String> = listOf("live", "viora", "vibes"),
    val isCoinGated: Boolean = false,
    val entryCoinPrice: Long = 0,
    val unlockedByUserIds: List<String> = emptyList()
)
