package com.example.model

data class VirtualGift(
    val id: String,
    val name: String,
    val emoji: String,
    val coinCost: Long,
    val description: String,
    val animationType: String = "POP",
    val soundEffect: String = "ding"
)

val DEFAULT_GIFTS = listOf(
    VirtualGift("gift_rose", "Rose", "🌹", 1, "A lovely crimson rose"),
    VirtualGift("gift_coffee", "Coffee", "☕", 5, "Fuel the broadcast"),
    VirtualGift("gift_heart", "Sparkle Heart", "💖", 10, "Warm glowing love"),
    VirtualGift("gift_fire", "Fire Energy", "🔥", 25, "The stream is lit!"),
    VirtualGift("gift_ring", "Diamond Ring", "💍", 50, "A pledge of devotion"),
    VirtualGift("gift_car", "Sports Car", "🏎️", 200, "High octane cruising"),
    VirtualGift("gift_rocket", "Galactic Rocket", "🚀", 500, "To the moon and beyond"),
    VirtualGift("gift_crown", "Viora Crown", "👑", 1000, "Royalty of the stream")
)

data class GiftTransaction(
    val id: String,
    val streamId: String,
    val gift: VirtualGift,
    val senderName: String,
    val senderAvatar: String,
    val recipientName: String,
    val count: Int = 1,
    val timestampMs: Long = System.currentTimeMillis()
)
