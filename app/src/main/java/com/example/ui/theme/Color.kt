package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// GLOBAL THEME & COLORS (Specified exact design tokens)
val VioraAppBg = Color(0xFF0D0D0D)
val VioraDarkBg = Color(0xFF0D0D0D)
val VioraSecondaryBg = Color(0xFF141414)
val VioraPlumAccentBg = Color(0xFF1A1423)

// Glassmorphism Surfaces & Cards
val VioraCardDark = Color(0xFF141414)
val VioraCardElevated = Color(0x0DFFFFFF) // rgba(255, 255, 255, 0.05)
val VioraGlassSurface = Color(0x0DFFFFFF) // rgba(255, 255, 255, 0.05)
val VioraGlassBorder = Color(0x1AFFFFFF) // rgba(255, 255, 255, 0.1)
val VioraStrongGlassSurface = Color(0x1AFFFFFF)

// Primary Accent (Gold): Gradient from #F5C04A to #D4A017
val VioraPrimaryGold = Color(0xFFF5C04A)
val VioraDarkGold = Color(0xFFD4A017)
val VioraSoftGold = Color(0xFFE5B038)
val VioraGoldHighlight = Color(0xFFFFF1BD)
val VioraGoldBorder = Color(0x80F5C04A)
val VioraGoldBorderSubtle = Color(0x33F5C04A)

// Secondary Accent (Neon Purple): Gradient from #9D4EDD to #E0AAFF
val VioraNeonPurple = Color(0xFF9D4EDD)
val VioraNeonPurpleLight = Color(0xFFE0AAFF)
val VioraTranslucentPurple = Color(0x339D4EDD) // rgba(157, 78, 221, 0.2)
val VioraPurpleGlow = Color(0x809D4EDD)

// Live Badge: Pink/Red gradient (#FF3366 to #FF6680)
val VioraLiveBadgeStart = Color(0xFFFF3366)
val VioraLiveBadgeEnd = Color(0xFFFF6680)
val VioraLiveCoralPink = Color(0xFFFF3366)

// Functional Colors
val VioraOnlineGreen = Color(0xFF35C98A)
val VioraWarningAmber = Color(0xFFE8A83E)
val VioraErrorRed = Color(0xFFFF5263)

// Text: White (#FFFFFF) for primary, Light Gray (#A0A0A0) for secondary
val VioraPrimaryText = Color(0xFFFFFFFF)
val VioraSecondaryText = Color(0xFFA0A0A0)
val VioraMutedText = Color(0xFF808080)
val VioraDisabledText = Color(0xFF5A5A5A)

// Legacy alias mappings for seamless compilation
val VioraCoral = VioraLiveCoralPink
val VioraWarmGold = VioraPrimaryGold
val VioraPrimaryChampagneGold = VioraPrimaryGold
val VioraDarkChampagneGold = VioraDarkGold
val VioraElectricPurple = VioraNeonPurple
val VioraSubtleViolet = VioraNeonPurple
val VioraDeepViolet = Color(0xFF3B1D5A)
val VioraPureWhite = VioraPrimaryText
val VioraBorder = VioraGlassBorder
val VioraSuccessGreen = VioraOnlineGreen
val VioraSurfaceVariant = VioraCardElevated
val VioraSubtleText = VioraSecondaryText

val VioraOverlayScrim = Color(0x80000000)
val VioraBadgeBg = Color(0xCC0D0D0D)
val VioraChatBubbleBg = Color(0x99141414)
