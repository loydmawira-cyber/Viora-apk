package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VioraDarkColorScheme = darkColorScheme(
    primary = VioraElectricPurple,
    onPrimary = VioraPureWhite,
    primaryContainer = VioraDeepViolet,
    onPrimaryContainer = VioraPurpleLight,
    secondary = VioraCoral,
    onSecondary = VioraPureWhite,
    secondaryContainer = Color(0xFF4A1828),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = VioraWarmGold,
    onTertiary = Color(0xFF2E1F00),
    tertiaryContainer = Color(0xFF4D3800),
    onTertiaryContainer = Color(0xFFFFE088),
    background = VioraDarkBg,
    onBackground = VioraPureWhite,
    surface = VioraCardDark,
    onSurface = VioraPureWhite,
    surfaceVariant = VioraCardElevated,
    onSurfaceVariant = VioraMutedText,
    outline = VioraBorder,
    error = VioraErrorRed,
    onError = VioraPureWhite
)

private val VioraLightColorScheme = lightColorScheme(
    primary = VioraElectricPurple,
    onPrimary = VioraPureWhite,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = VioraDeepViolet,
    secondary = VioraCoral,
    onSecondary = VioraPureWhite,
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF410011),
    tertiary = Color(0xFFC79200),
    onTertiary = VioraPureWhite,
    background = Color(0xFFFAF7FD),
    onBackground = Color(0xFF1B1526),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1526),
    surfaceVariant = Color(0xFFECE6F2),
    onSurfaceVariant = Color(0xFF5A5266),
    outline = Color(0xFFC9C1D6),
    error = VioraErrorRed,
    onError = VioraPureWhite
)

@Composable
fun VioraTheme(
    darkTheme: Boolean = true, // Dark mode is primary for live streaming experience
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VioraDarkColorScheme else VioraLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
