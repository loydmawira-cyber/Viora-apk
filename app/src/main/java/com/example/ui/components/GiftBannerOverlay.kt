package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GiftTransaction
import com.example.ui.theme.VioraCardElevated
import com.example.ui.theme.VioraCoral
import com.example.ui.theme.VioraDeepViolet
import com.example.ui.theme.VioraElectricPurple
import com.example.ui.theme.VioraWarmGold
import kotlinx.coroutines.delay

@Composable
fun GiftBannerOverlay(
    activeBanner: GiftTransaction?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(activeBanner) {
        if (activeBanner != null) {
            delay(3500)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = activeBanner != null,
        enter = slideInVertically(animationSpec = tween(400)) { -it } + fadeIn(),
        exit = slideOutVertically(animationSpec = tween(400)) { -it } + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (activeBanner != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                VioraDeepViolet.copy(alpha = 0.95f),
                                VioraElectricPurple.copy(alpha = 0.95f),
                                VioraCardElevated.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(VioraWarmGold, VioraCoral)),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarView(
                    imageUrl = activeBanner.senderAvatar,
                    displayName = activeBanner.senderName,
                    size = 36.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeBanner.senderName,
                        color = VioraWarmGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "sent ${activeBanner.gift.name} to ${activeBanner.recipientName}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = activeBanner.gift.emoji,
                        fontSize = 28.sp
                    )
                    Text(
                        text = "x${activeBanner.count}",
                        color = VioraCoral,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
