package com.example.ludo.core.util

import androidx.compose.ui.graphics.Color
import com.example.ludo.model.PlayerColor
import com.example.ludo.theme.*

/** Shading ramp for pawns (highlight → base → outline → shadow), allocated once per colour. */
data class PawnColorScheme(
    val highlightColor: Color,
    val lightColor: Color,
    val baseColor: Color,
    val darkColor: Color,
    val deepShadow: Color
)

object PlayerColorUtils {

    val RedPawnScheme = PawnColorScheme(
        highlightColor = Color(0xFFFFB3AC),
        lightColor = Color(0xFFF26D6D),
        baseColor = Color(0xFFE23B3B),
        darkColor = Color(0xFFA82525),
        deepShadow = Color(0xFF6E1414)
    )

    val GreenPawnScheme = PawnColorScheme(
        highlightColor = Color(0xFFA8EEC5),
        lightColor = Color(0xFF4CC484),
        baseColor = Color(0xFF1FA35B),
        darkColor = Color(0xFF137040),
        deepShadow = Color(0xFF0B4527)
    )

    val YellowPawnScheme = PawnColorScheme(
        highlightColor = Color(0xFFFFEBA3),
        lightColor = Color(0xFFFFCF3D),
        baseColor = Color(0xFFF2B200),
        darkColor = Color(0xFFB88400),
        deepShadow = Color(0xFF7A5700)
    )

    val BluePawnScheme = PawnColorScheme(
        highlightColor = Color(0xFFB3D6FF),
        lightColor = Color(0xFF5C9FEB),
        baseColor = Color(0xFF2B7FE0),
        darkColor = Color(0xFF1B58A3),
        deepShadow = Color(0xFF103767)
    )

    // Indexed by PlayerColor.ordinal for allocation-free lookups in draw loops.
    private val pawnSchemes = arrayOf(RedPawnScheme, GreenPawnScheme, YellowPawnScheme, BluePawnScheme)
    private val composeColors = arrayOf(LudoRed, LudoGreen, LudoYellow, LudoBlue)
    private val lightColors = arrayOf(LudoRedLight, LudoGreenLight, LudoYellowLight, LudoBlueLight)

    fun getPawnColorScheme(color: PlayerColor): PawnColorScheme = pawnSchemes[color.ordinal]

    fun getComposeColor(color: PlayerColor?): Color = color?.let { composeColors[it.ordinal] } ?: LudoGreen

    fun getLightColor(color: PlayerColor?): Color = color?.let { lightColors[it.ordinal] } ?: LudoGreenLight
}
