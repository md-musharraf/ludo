package com.example.ludo.core.util

import androidx.compose.ui.graphics.Color
import com.example.ludo.model.PlayerColor
import com.example.ludo.theme.*

/**
 * Static Color Schemes for 3D Photorealistic Pawns.
 * Pre-allocated to guarantee zero allocation overhead during 60-120fps render loops.
 */
data class PawnColorScheme(
    val highlightColor: Color,
    val lightColor: Color,
    val baseColor: Color,
    val darkColor: Color,
    val deepShadow: Color
)

object PlayerColorUtils {

    val RedPawnScheme = PawnColorScheme(
        highlightColor = Color(0xFFFF8A80),
        lightColor = Color(0xFFFF5252),
        baseColor = Color(0xFFD32F2F),
        darkColor = Color(0xFFB71C1C),
        deepShadow = Color(0xFF4A0000)
    )

    val GreenPawnScheme = PawnColorScheme(
        highlightColor = Color(0xFFB9F6CA),
        lightColor = Color(0xFF4CAF50),
        baseColor = Color(0xFF2E7D32),
        darkColor = Color(0xFF1B5E20),
        deepShadow = Color(0xFF002900)
    )

    val YellowPawnScheme = PawnColorScheme(
        highlightColor = Color(0xFFFFF9C4),
        lightColor = Color(0xFFFFD54F),
        baseColor = Color(0xFFFBC02D),
        darkColor = Color(0xFFF57F17),
        deepShadow = Color(0xFF6D3600)
    )

    val BluePawnScheme = PawnColorScheme(
        highlightColor = Color(0xFFB3E5FC),
        lightColor = Color(0xFF42A5F5),
        baseColor = Color(0xFF1976D2),
        darkColor = Color(0xFF0D47A1),
        deepShadow = Color(0xFF00153B)
    )

    private val pawnSchemesMap = mapOf(
        PlayerColor.RED to RedPawnScheme,
        PlayerColor.GREEN to GreenPawnScheme,
        PlayerColor.YELLOW to YellowPawnScheme,
        PlayerColor.BLUE to BluePawnScheme
    )

    fun getPawnColorScheme(color: PlayerColor): PawnColorScheme {
        return pawnSchemesMap[color] ?: RedPawnScheme
    }

    fun getComposeColor(color: PlayerColor?): Color {
        return when (color) {
            PlayerColor.RED -> LudoRed
            PlayerColor.GREEN -> LudoGreen
            PlayerColor.YELLOW -> LudoYellow
            PlayerColor.BLUE -> LudoBlue
            null -> LudoGreen
        }
    }

    fun getLightColor(color: PlayerColor?): Color {
        return when (color) {
            PlayerColor.RED -> LudoRedLight
            PlayerColor.GREEN -> LudoGreenLight
            PlayerColor.YELLOW -> LudoYellowLight
            PlayerColor.BLUE -> LudoBlueLight
            null -> LudoGreenLight
        }
    }
}
