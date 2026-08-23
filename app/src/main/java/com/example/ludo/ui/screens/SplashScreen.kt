package com.example.ludo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var isStarted by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Dice rotation and bobbing
    val diceRotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "diceRotation"
    )

    val diceScale by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "diceScale"
    )

    val titleScale by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "titleScale"
    )

    LaunchedEffect(Unit) {
        isStarted = true
        delay(2400)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8E1),
                        Color(0xFFFFECB3),
                        Color(0xFFFFE082)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated 4-Color Dice Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .scale(diceScale)
                    .rotate(diceRotation)
                    .shadow(16.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .border(3.dp, Color(0xFFE0E0E0), RoundedCornerShape(32.dp))
            ) {
                // 4 Quadrants inside emblem
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(LudoRed))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(LudoGreen))
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(LudoBlue))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(LudoYellow))
                    }
                }

                // Center Gold Token Emblem
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Color(0xFFFFD700), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎲", fontSize = 26.sp)
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Animated Letters: L - U - D - O
            Row(
                modifier = Modifier.scale(titleScale),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashLetter("L", LudoRed)
                SplashLetter("U", LudoGreen)
                SplashLetter("D", LudoYellow)
                SplashLetter("O", LudoBlue)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SUPER LUDO MASTER",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6D4C41),
                letterSpacing = 3.sp
            )
        }
    }
}

@Composable
private fun SplashLetter(char: String, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .border(2.dp, Color.White, RoundedCornerShape(16.dp))
    ) {
        Text(
            text = char,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}
