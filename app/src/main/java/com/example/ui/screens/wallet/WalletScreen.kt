package com.example.ui.screens.wallet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VioraRepository
import com.example.model.COIN_PACKAGES
import com.example.model.CoinPackage
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.example.ui.theme.*

@Composable
fun WalletScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val currentUser by VioraRepository.currentUser.collectAsState()
    val ledger by VioraRepository.ledger.collectAsState()
    val withdrawals by VioraRepository.withdrawals.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Buy Coins, 1: Creator Earnings, 2: History
    var showCashOutDialog by remember { mutableStateOf(false) }
    var successToastMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VioraCardDark)
                    .testTag("wallet_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = "Viora Wallet & Earnings",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Balance Overview Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Coins Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = VioraCardDark),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = VioraWarmGold, modifier = Modifier.size(18.dp))
                        Text("Coins", color = VioraMutedText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${currentUser.coinBalance}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                    Text("Available to spend", color = VioraSubtleText, fontSize = 10.sp)
                }
            }

            // Diamonds / Earnings Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = VioraDeepViolet),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("💎", fontSize = 14.sp)
                        Text("Earnings", color = VioraWarmGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${currentUser.diamondBalance}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "≈ $%.2f USD".format(currentUser.diamondBalance / 100.0),
                        color = VioraSuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(VioraCardDark)
                .padding(4.dp)
        ) {
            listOf("Buy Coins", "Creator Cash Out", "Ledger History").forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) VioraElectricPurple else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else VioraMutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content per tab
        when (selectedTab) {
            0 -> BuyCoinsView(
                onBuyPackage = { pkg ->
                    VioraRepository.buyCoinPackage(pkg)
                    successToastMessage = if (com.example.data.SupabaseApi.isConfigured) {
                        "Coin purchases are not open yet."
                    } else {
                        "Purchased ${pkg.coins + pkg.bonusCoins} coins successfully!"
                    }
                }
            )
            1 -> CreatorEarningsView(
                diamondBalance = currentUser.diamondBalance,
                withdrawals = withdrawals,
                onOpenCashOut = { showCashOutDialog = true }
            )
            2 -> LedgerHistoryView(ledger = ledger)
        }
    }

    // Cash Out Modal Dialog
    if (showCashOutDialog) {
        var withdrawAmountText by remember { mutableStateOf("${currentUser.diamondBalance}") }
        var selectedMethod by remember { mutableStateOf("PayPal") }
        var destinationAccount by remember { mutableStateOf("creator.payout@gmail.com") }

        AlertDialog(
            onDismissRequest = { showCashOutDialog = false },
            title = { Text("Request Cash Out", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Exchange Rate: 100 Diamonds = $1.00 USD (Min: 500)", color = VioraWarmGold, fontSize = 12.sp)

                    OutlinedTextField(
                        value = withdrawAmountText,
                        onValueChange = { withdrawAmountText = it },
                        label = { Text("Diamonds to Cash Out") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioraElectricPurple,
                            unfocusedBorderColor = VioraBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Text("Select Payout Provider:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PayPal", "Stripe", "Bank Wire").forEach { method ->
                            val isSelected = selectedMethod == method
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) VioraCoral else VioraSurfaceVariant)
                                    .clickable { selectedMethod = method }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(method, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = destinationAccount,
                        onValueChange = { destinationAccount = it },
                        label = { Text("Payout Email or Account #") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioraElectricPurple,
                            unfocusedBorderColor = VioraBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = withdrawAmountText.toLongOrNull() ?: 0L
                        val res = VioraRepository.requestWithdrawal(amount, selectedMethod, destinationAccount)
                        if (res.isSuccess) {
                            showCashOutDialog = false
                            successToastMessage = "Withdrawal request submitted for review!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioraSuccessGreen)
                ) {
                    Text("Confirm Payout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashOutDialog = false }) {
                    Text("Cancel", color = VioraMutedText)
                }
            },
            containerColor = VioraCardDark
        )
    }
}

@Composable
private fun BuyCoinsView(onBuyPackage: (CoinPackage) -> Unit) {
    val packages by VioraRepository.coinPackages.collectAsState()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(packages.filter { it.isActive }) { pkg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onBuyPackage(pkg) }
                    .testTag("coin_pkg_${pkg.id}"),
                colors = CardDefaults.cardColors(containerColor = VioraCardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(VioraCardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = VioraWarmGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${pkg.coins} Coins",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (pkg.bonusCoins > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(VioraCoral)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("+${pkg.bonusCoins}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text("Official Google Play / App Store IAP", color = VioraSubtleText, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = { onBuyPackage(pkg) },
                        colors = ButtonDefaults.buttonColors(containerColor = VioraElectricPurple),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(pkg.priceUsd, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorEarningsView(
    diamondBalance: Long,
    withdrawals: List<com.example.model.WithdrawalRequest>,
    onOpenCashOut: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VioraCardDark),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Creator Revenue Share: 70%", color = VioraWarmGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = "Creators earn diamonds every time viewers send virtual gifts during live broadcasts. Diamonds can be redeemed directly to PayPal or Bank Account.",
                    color = VioraMutedText,
                    fontSize = 12.sp
                )

                Button(
                    onClick = onOpenCashOut,
                    enabled = diamondBalance >= 500,
                    colors = ButtonDefaults.buttonColors(containerColor = VioraCoral),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cash_out_button")
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (diamondBalance >= 500) "Request Cash Out ($%.2f USD)".format(diamondBalance / 100.0) else "Min 500 Diamonds to Cash Out",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text("Recent Withdrawal Requests", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        if (withdrawals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No payout requests yet.", color = VioraMutedText, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(withdrawals) { req ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VioraCardDark)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${req.diamondsAmount} Diamonds (${req.usdValue})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Via ${req.payoutMethod} • ${req.payoutDestination}", color = VioraMutedText, fontSize = 11.sp)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VioraWarmGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(req.status.name, color = VioraWarmGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerHistoryView(ledger: List<com.example.model.LedgerTransaction>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(ledger) { tx ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VioraCardDark)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (tx.type) {
                                    TransactionType.COIN_PURCHASE -> VioraSuccessGreen.copy(alpha = 0.2f)
                                    TransactionType.GIFT_SENT -> VioraCoral.copy(alpha = 0.2f)
                                    TransactionType.GIFT_RECEIVED -> VioraWarmGold.copy(alpha = 0.2f)
                                    TransactionType.WITHDRAWAL -> VioraElectricPurple.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (tx.type) {
                                TransactionType.COIN_PURCHASE -> Icons.Default.AddCircle
                                TransactionType.GIFT_SENT -> Icons.Default.CardGiftcard
                                TransactionType.GIFT_RECEIVED -> Icons.Default.VolunteerActivism
                                TransactionType.WITHDRAWAL -> Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            tint = when (tx.type) {
                                TransactionType.COIN_PURCHASE -> VioraSuccessGreen
                                TransactionType.GIFT_SENT -> VioraCoral
                                TransactionType.GIFT_RECEIVED -> VioraWarmGold
                                TransactionType.WITHDRAWAL -> VioraElectricPurple
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(tx.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(tx.type.name, color = VioraMutedText, fontSize = 10.sp)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (tx.amountCoins != 0L) {
                        Text(
                            text = if (tx.amountCoins > 0) "+${tx.amountCoins} Coins" else "${tx.amountCoins} Coins",
                            color = if (tx.amountCoins > 0) VioraSuccessGreen else VioraCoral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    if (tx.amountDiamonds != 0L) {
                        Text(
                            text = if (tx.amountDiamonds > 0) "+${tx.amountDiamonds} 💎" else "${tx.amountDiamonds} 💎",
                            color = VioraWarmGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    if (tx.amountUsd.isNotBlank()) {
                        Text(tx.amountUsd, color = VioraMutedText, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
