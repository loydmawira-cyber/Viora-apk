package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.LiveStream
import com.example.ui.theme.*

private val GRADIENT_PRESETS = listOf(
    listOf(Color(0xFF19131D), Color(0xFF211E24), Color(0xFF3B2675)),
    listOf(Color(0xFF0B0A0D), Color(0xFF1A171D), Color(0xFF2C1C38)),
    listOf(Color(0xFF151218), Color(0xFF281C30), Color(0xFF100E13)),
    listOf(Color(0xFF1F1C2C), Color(0xFF32233E), Color(0xFF1A171D))
)

@Composable
fun StreamCard(
    stream: LiveStream,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = GRADIENT_PRESETS.getOrElse(stream.thumbnailGradientIndex % GRADIENT_PRESETS.size) {
        listOf(VioraCardDark, VioraCardElevated)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, VioraGoldBorderSubtle, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("stream_card_${stream.id}"),
        colors = CardDefaults.cardColors(containerColor = VioraCardDark),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Live Preview Thumbnail Snapshot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
            ) {
                if (stream.snapshotUrl.isNotBlank()) {
                    AsyncImage(
                        model = stream.snapshotUrl,
                        contentDescription = "Live snapshot of ${stream.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(colors = gradientColors)
                            )
                    )
                }

                // Gradient scrim overlay for high contrast and readability of badges & metrics
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Top row badges: LIVE badge on left + Viewer count on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveBadge()
                        if (stream.isCoinGated) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VioraPrimaryChampagneGold)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "VIP Ticket",
                                        tint = Color.Black,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${stream.entryCoinPrice} Coins",
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    // Viewer Count Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Viewers",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${stream.viewerCount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom-left pills: Country flag badge + Category pill
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Country Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.70f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(text = stream.countryFlag, fontSize = 12.sp)
                            Text(
                                text = stream.countryCode,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Category Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.70f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = stream.category.title,
                            color = VioraPrimaryChampagneGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Diamonds collected pill on bottom-right
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(text = "💎", fontSize = 10.sp)
                    Text(
                        text = "${stream.diamondCount}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Creator & Stream Details Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarView(
                    imageUrl = stream.hostAvatar,
                    displayName = stream.hostName,
                    size = 40.dp,
                    showBorder = true,
                    borderColor = VioraPrimaryChampagneGold,
                    isVerified = true
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stream.title,
                        color = VioraPrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stream.hostName,
                            color = VioraSecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "• ${stream.countryFlag} ${stream.countryName}",
                            color = VioraMutedText,
                            fontSize = 11.sp
                        )
                    }
                }

                // Follow / Action Button (Circular Deep Purple with Heart icon)
                IconButton(
                    onClick = onToggleFollow,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (stream.isFollowing) VioraCardElevated else VioraDeepViolet)
                        .border(1.dp, if (stream.isFollowing) VioraGoldBorderSubtle else VioraSubtleViolet, CircleShape)
                ) {
                    Icon(
                        imageVector = if (stream.isFollowing) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (stream.isFollowing) "Unfollow" else "Follow",
                        tint = if (stream.isFollowing) VioraLiveCoralPink else VioraPrimaryChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
