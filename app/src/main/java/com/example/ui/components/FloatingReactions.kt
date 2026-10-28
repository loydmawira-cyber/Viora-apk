package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.theme.VioraCoral
import com.example.ui.theme.VioraElectricPurple
import com.example.ui.theme.VioraWarmGold
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.random.Random

data class FloatingParticle(
    val id: String = UUID.randomUUID().toString(),
    val startXRatio: Float = Random.nextFloat() * 0.4f + 0.5f,
    val color: Color = listOf(VioraCoral, VioraElectricPurple, VioraWarmGold, Color.White).random(),
    val scale: Float = Random.nextFloat() * 0.6f + 0.8f,
    val swayAmplitude: Float = (Random.nextFloat() * 40f + 20f) * (if (Random.nextBoolean()) 1f else -1f),
    val creationTime: Long = System.currentTimeMillis()
)

@Composable
fun FloatingReactionsOverlay(
    triggerHeartCount: Int,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableStateListOf<FloatingParticle>() }

    LaunchedEffect(triggerHeartCount) {
        if (triggerHeartCount > 0) {
            val countToAdd = (1..3).random()
            repeat(countToAdd) {
                particles.add(FloatingParticle())
            }
        }
    }

    // Ticking animation to prune expired particles
    LaunchedEffect(particles.size) {
        while (particles.isNotEmpty()) {
            delay(100)
            val now = System.currentTimeMillis()
            particles.removeAll { now - it.creationTime > 2500 }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particle_tick")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "tick"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val now = System.currentTimeMillis()
        val width = size.width
        val height = size.height

        particles.forEach { particle ->
            val progress = ((now - particle.creationTime) / 2500f).coerceIn(0f, 1f)
            val currentY = height * (1f - progress * 0.85f)
            val sway = kotlin.math.sin(progress * Math.PI.toFloat() * 3f) * particle.swayAmplitude
            val currentX = width * particle.startXRatio + sway
            val alpha = (1f - progress).coerceIn(0f, 1f)

            drawHeart(
                center = Offset(currentX, currentY),
                size = 28f * particle.scale,
                color = particle.color.copy(alpha = alpha)
            )
        }
    }
}

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        val x = center.x
        val y = center.y
        moveTo(x, y + size * 0.6f)
        cubicTo(x - size, y + size * 0.1f, x - size, y - size * 0.6f, x, y - size * 0.2f)
        cubicTo(x + size, y - size * 0.6f, x + size, y + size * 0.1f, x, y + size * 0.6f)
        close()
    }
    drawPath(path = path, color = color)
}
