package com.example.ludo.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var isStarted by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val diceRotation = infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "diceRotation"
    )

    val diceScale = animateFloatAsState(
        targetValue = if (isStarted) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "diceScale"
    )

    val titleScale = animateFloatAsState(
        targetValue = if (isStarted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "titleScale"
    )

    LaunchedEffect(Unit) {
        isStarted = true
        delay(1800)
        onSplashFinished()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        val isCompact = maxHeight < 680.dp
        val emblemSize = if (isCompact) 105.dp else 130.dp
        val centerDiceSize = if (isCompact) 44.dp else 54.dp
        val centerDiceFont = if (isCompact) 20.sp else 26.sp
        val letterBoxSize = if (isCompact) 46.dp else 56.dp
        val letterFontSize = if (isCompact) 26.sp else 32.sp
        val verticalSpacer = if (isCompact) 20.dp else 36.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 4-Quadrant Colored Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(emblemSize)
                    .graphicsLayer {
                        scaleX = diceScale.value
                        scaleY = diceScale.value
                        rotationZ = diceRotation.value
                    }
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .border(3.dp, Color(0xFFE0E0E0), RoundedCornerShape(28.dp))
            ) {
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

                Box(
                    modifier = Modifier
                        .size(centerDiceSize)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Color(0xFFFFD700), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎲", fontSize = centerDiceFont)
                }
            }

            Spacer(modifier = Modifier.height(verticalSpacer))

            Row(
                modifier = Modifier.graphicsLayer {
                    scaleX = titleScale.value
                    scaleY = titleScale.value
                },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashLetter("L", LudoRed, letterBoxSize, letterFontSize)
                SplashLetter("U", LudoGreen, letterBoxSize, letterFontSize)
                SplashLetter("D", LudoYellow, letterBoxSize, letterFontSize)
                SplashLetter("O", LudoBlue, letterBoxSize, letterFontSize)
            }

            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

            Text(
                text = "SUPER LUDO MASTER",
                fontSize = if (isCompact) 11.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextBrown,
                letterSpacing = if (isCompact) 2.5.sp else 3.sp
            )
        }
    }
}

@Composable
private fun SplashLetter(char: String, color: Color, size: Dp = 56.dp, fontSize: TextUnit = 32.sp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .border(2.dp, Color.White, RoundedCornerShape(14.dp))
    ) {
        Text(
            text = char,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

