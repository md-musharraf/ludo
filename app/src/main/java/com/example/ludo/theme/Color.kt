package com.example.ludo.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Classic Ludo vibrant colors matching reference
val LudoRed = Color(0xFFE52521)
val LudoGreen = Color(0xFF009E3D)
val LudoYellow = Color(0xFFE5A800)
val LudoBlue = Color(0xFF00A3FF)

val LudoRedLight = Color(0xFFFFCDD2)
val LudoGreenLight = Color(0xFFC8E6C9)
val LudoYellowLight = Color(0xFFFFF9C4)
val LudoBlueLight = Color(0xFFBBDEFB)

val SafeZoneStar = Color(0xFFFFFFFF)

// Common UI support
val CardWarm = Color(0xFFFFFFFF)
val CardBorderWarm = Color(0xFFE0E0E0)
val TextDark = Color(0xFF3E2723)
val TextMuted = Color(0xFF757575)
val TextBrown = Color(0xFF6D4C41)

/** Warm parchment backdrop shared by every screen. */
val WarmBackgroundBrush = Brush.verticalGradient(
    listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3), Color(0xFFFFE082))
)
