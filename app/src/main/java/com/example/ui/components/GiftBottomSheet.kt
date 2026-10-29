package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DEFAULT_GIFTS
import com.example.model.VirtualGift
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftBottomSheet(
    userCoinBalance: Long,
    onSendGift: (VirtualGift, Int) -> Unit,
    onNavigateToRecharge: () -> Unit,
    onDismiss: () -> Unit
) {
    val catalog by com.example.data.VioraRepository.giftCatalog.collectAsState()
    val activeGifts = catalog.filter { it.isActive }
    var selectedGift by remember { mutableStateOf<VirtualGift?>(DEFAULT_GIFTS.firstOrNull()) }
    var selectedCombo by remember { mutableIntStateOf(1) }
    val comboOptions = listOf(1, 5, 10, 99)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VioraCardDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = VioraBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header: Balance + Recharge Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = VioraWarmGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "$userCoinBalance",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Coins",
                        color = VioraMutedText,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onNavigateToRecharge,
                    colors = ButtonDefaults.buttonColors(containerColor = VioraElectricPurple),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("recharge_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recharge", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Gifts Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                items(activeGifts) { gift ->
                    val isSelected = selectedGift?.id == gift.id
                    val canAfford = userCoinBalance >= (gift.coinCost * selectedCombo)

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) VioraSurfaceVariant else VioraCardElevated)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) VioraWarmGold else VioraBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedGift = gift }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = gift.emoji,
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = gift.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = VioraWarmGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${gift.coinCost}",
                                color = if (canAfford) VioraWarmGold else VioraCoral,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Combo selection & Send Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Combo pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    comboOptions.forEach { count ->
                        val isComboSelected = selectedCombo == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isComboSelected) VioraElectricPurple else VioraSurfaceVariant)
                                .clickable { selectedCombo = count }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "x$count",
                                color = if (isComboSelected) Color.White else VioraMutedText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Send Button
                val currentCost = (selectedGift?.coinCost ?: 0) * selectedCombo
                val hasSufficientCoins = userCoinBalance >= currentCost

                Button(
                    onClick = {
                        selectedGift?.let { onSendGift(it, selectedCombo) }
                    },
                    enabled = selectedGift != null && hasSufficientCoins,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VioraCoral,
                        disabledContainerColor = VioraSurfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("send_gift_button")
                ) {
                    Text(
                        text = if (hasSufficientCoins) "Send ($currentCost)" else "Need Coins",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
