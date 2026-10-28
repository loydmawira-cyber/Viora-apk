package com.example.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class VioraNotification(
    val id: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val iconEmoji: String,
    val isUnread: Boolean = false
)

private val SAMPLE_NOTIFICATIONS = listOf(
    VioraNotification("notif_1", "Chloe Beats is LIVE!", "Acoustic Sunset Jam & Song Requests 🎸 Tap to join the live room.", "10m ago", "🔴", true),
    VioraNotification("notif_2", "Gift Received!", "You received Galactic Rocket 🚀 from Sara Croft (+350 Diamonds)", "1h ago", "🎁", true),
    VioraNotification("notif_3", "New Follower", "Neon K followed your profile.", "3h ago", "✨", false),
    VioraNotification("notif_4", "Creator Payout Approved", "Your withdrawal request of $45.00 via PayPal has been processed.", "1d ago", "💰", false),
    VioraNotification("notif_5", "Community Safety Notice", "Viora updated live streaming safety standards. Check community rules.", "2d ago", "🛡️", false)
)

@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VioraDarkBg)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Text(
                text = "Mark all read",
                color = VioraCoral,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(SAMPLE_NOTIFICATIONS) { notif ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isUnread) VioraCardElevated else VioraCardDark
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VioraSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(notif.iconEmoji, fontSize = 20.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = notif.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = notif.timeAgo,
                                    color = VioraMutedText,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = notif.body,
                                color = VioraMutedText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
