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
    listOf(Color(0xFF141414), Color(0xFF1E1428), Color(0xFF2B173D)),
    listOf(Color(0xFF0D0D0D), Color(0xFF191420), Color(0xFF241830)),
    listOf(Color(0xFF141414), Color(0xFF1D1B22), Color(0xFF0D0D0D))
)

@Composable
fun StreamCard(
    stream: LiveStream,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = GRADIENT_PRESETS.getOrElse(stream.thumbnailGradientIndex % GRADIENT_PRESETS.size) {
        listOf(VioraCardDark, VioraPlumAccentBg)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, VioraGlassBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("stream_card_${stream.id}"),
        colors = CardDefaults.cardColors(containerColor = VioraCardDark),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Media Area (Top)
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

                // Scrim Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.80f)
                                )
                            )
                        )
                )

                // Top Badges
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
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(VioraPrimaryGold, VioraDarkGold)
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
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

                    // Top-Right: Glassmorphism pill with eye icon and viewer count
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(VioraGlassSurface)
                            .border(1.dp, VioraGlassBorder, RoundedCornerShape(12.dp))
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

                // Bottom-Left: Glassmorphism pills with country flag/code and category
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VioraGlassSurface)
                            .border(1.dp, VioraGlassBorder, RoundedCornerShape(8.dp))
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

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VioraGlassSurface)
                            .border(1.dp, VioraGlassBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = stream.category.title,
                            color = VioraPrimaryGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom-Right: Glassmorphism pill with blue diamond icon and gem count
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VioraGlassSurface)
                        .border(1.dp, VioraGlassBorder, RoundedCornerShape(8.dp))
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

            // Info Area (Bottom)
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
                    size = 42.dp,
                    showBorder = true,
                    borderColor = VioraPrimaryGold,
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
                            color = VioraSecondaryText,
                            fontSize = 11.sp
                        )
                    }
                }

                // Right: Large circular button with translucent purple background, neon purple border glow, and a heart icon
                IconButton(
                    onClick = onToggleFollow,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (stream.isFollowing) VioraTranslucentPurple else VioraTranslucentPurple
                        )
                        .border(
                            1.5.dp,
                            if (stream.isFollowing) VioraNeonPurpleLight else VioraNeonPurple,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (stream.isFollowing) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (stream.isFollowing) "Unfollow" else "Follow",
                        tint = if (stream.isFollowing) VioraLiveBadgeStart else VioraNeonPurpleLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
