package com.example.ludo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.theme.AppBackground
import com.example.ludo.theme.InkMuted
import com.example.ludo.ui.components.LogoMark
import com.example.ludo.ui.components.LudoTitle
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val logo = remember { Animatable(0.6f) }
    val text = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val bouncy = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        logo.animateTo(1f, bouncy)
        text.animateTo(1f, tween(350))
        delay(700)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(
                size = 104.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = logo.value
                    scaleY = logo.value
                    alpha = ((logo.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                }
            )
            Spacer(Modifier.height(24.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = text.value
                    translationY = (1f - text.value) * 24f
                }
            ) {
                LudoTitle(fontSize = 48.sp)
                Text("The classic board game", fontSize = 14.sp, color = InkMuted, fontWeight = FontWeight.Medium)
            }
        }
    }
}
