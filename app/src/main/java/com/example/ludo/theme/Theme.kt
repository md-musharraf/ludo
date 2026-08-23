package com.example.ludo.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClassicScheme = lightColorScheme(
    primary = LudoGreen,
    secondary = LudoBlue,
    tertiary = LudoYellow,
    background = BoardBackground,
    surface = CardWarm,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun LudoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClassicScheme,
        typography = Typography,
        content = content
    )
}
