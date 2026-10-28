package com.example.model

enum class TransactionType {
    COIN_PURCHASE,
    GIFT_SENT,
    GIFT_RECEIVED,
    WITHDRAWAL
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REJECTED
}

data class CoinPackage(
    val id: String,
    val coins: Long,
    val bonusCoins: Long = 0,
    val priceUsd: String
)

val COIN_PACKAGES = listOf(
    CoinPackage("pkg_100", 100, 0, "$0.99"),
    CoinPackage("pkg_500", 500, 50, "$4.99"),
    CoinPackage("pkg_1200", 1200, 150, "$9.99"),
    CoinPackage("pkg_3000", 3000, 500, "$24.99"),
    CoinPackage("pkg_6500", 6500, 1200, "$49.99"),
    CoinPackage("pkg_15000", 15000, 3500, "$99.99")
)

data class LedgerTransaction(
    val id: String,
    val type: TransactionType,
    val title: String,
    val amountCoins: Long = 0,
    val amountDiamonds: Long = 0,
    val amountUsd: String = "",
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val timestampMs: Long = System.currentTimeMillis(),
    val referenceId: String = ""
)

data class WithdrawalRequest(
    val id: String,
    val userId: String,
    val diamondsAmount: Long,
    val usdValue: String,
    val payoutMethod: String,
    val payoutDestination: String,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val requestedAtEpochMs: Long = System.currentTimeMillis()
)
