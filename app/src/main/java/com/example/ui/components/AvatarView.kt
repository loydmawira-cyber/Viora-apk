package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.VioraCoral
import com.example.ui.theme.VioraDeepViolet
import com.example.ui.theme.VioraElectricPurple
import com.example.ui.theme.VioraWarmGold

@Composable
fun AvatarView(
    imageUrl: String,
    displayName: String,
    size: Dp = 44.dp,
    showBorder: Boolean = false,
    borderColor: Color = VioraElectricPurple,
    isVerified: Boolean = false,
    level: Int? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val borderModifier = if (showBorder) {
            Modifier.border(
                2.dp,
                Brush.sweepGradient(listOf(VioraElectricPurple, VioraCoral, VioraWarmGold, VioraElectricPurple)),
                CircleShape
            )
        } else Modifier

        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "$displayName's avatar",
                modifier = Modifier
                    .matchParentSize()
                    .then(borderModifier)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback gradient monogram
            val initial = displayName.firstOrNull()?.uppercase() ?: "V"
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(borderModifier)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(VioraDeepViolet, VioraElectricPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42f).sp
                )
            }
        }

        if (isVerified) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = VioraWarmGold,
                modifier = Modifier
                    .size((size.value * 0.35f).dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            )
        }
    }
}
