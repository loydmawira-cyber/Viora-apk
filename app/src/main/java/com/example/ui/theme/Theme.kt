package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VioraDarkColorScheme = darkColorScheme(
    primary = VioraPrimaryChampagneGold,
    onPrimary = VioraAppBg,
    primaryContainer = VioraDeepViolet,
    onPrimaryContainer = VioraGoldHighlight,
    secondary = VioraLiveCoralPink,
    onSecondary = VioraPrimaryText,
    secondaryContainer = Color(0xFF381423),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = VioraSoftGold,
    onTertiary = VioraAppBg,
    tertiaryContainer = VioraDarkChampagneGold,
    onTertiaryContainer = VioraGoldHighlight,
    background = VioraAppBg,
    onBackground = VioraPrimaryText,
    surface = VioraCardDark,
    onSurface = VioraPrimaryText,
    surfaceVariant = VioraCardElevated,
    onSurfaceVariant = VioraSecondaryText,
    outline = VioraGoldBorder,
    error = VioraErrorRed,
    onError = VioraPrimaryText
)

private val VioraLightColorScheme = lightColorScheme(
    primary = VioraPrimaryChampagneGold,
    onPrimary = VioraAppBg,
    primaryContainer = Color(0xFFF3E7CA),
    onPrimaryContainer = Color(0xFF382A0B),
    secondary = VioraLiveCoralPink,
    onSecondary = VioraPrimaryText,
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF410011),
    tertiary = VioraSoftGold,
    onTertiary = VioraPrimaryText,
    background = Color(0xFF151218),
    onBackground = VioraPrimaryText,
    surface = VioraCardDark,
    onSurface = VioraPrimaryText,
    surfaceVariant = VioraCardElevated,
    onSurfaceVariant = VioraSecondaryText,
    outline = VioraGoldBorder,
    error = VioraErrorRed,
    onError = VioraPrimaryText
)

@Composable
fun VioraTheme(
    darkTheme: Boolean = true, // Black + Champagne Gold futuristic theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VioraDarkColorScheme else VioraLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
