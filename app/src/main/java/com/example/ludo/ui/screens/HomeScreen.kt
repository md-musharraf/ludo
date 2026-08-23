package com.example.ludo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.theme.*

@Composable
fun HomeScreen(onStartGame: (Int, Boolean, String) -> Unit) {
    var playerCount by remember { mutableIntStateOf(4) }
    var isVsAI by remember { mutableStateOf(true) }
    var aiDifficulty by remember { mutableStateOf("Hard") }

    // Animated background circles
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bgRotation"
    )

    // Button press animation
    var startPressed by remember { mutableStateOf(false) }
    val startScale by animateFloatAsState(
        targetValue = if (startPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "startScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8E1),
                        Color(0xFFFFF3E0),
                        Color(0xFFFFECB3)
                    )
                )
            )
    ) {
        // Decorative background circles
        val decorCircles = remember {
            listOf(
                Triple(0.1f, 0.15f, LudoRed.copy(alpha = 0.08f)),
                Triple(0.85f, 0.1f, LudoGreen.copy(alpha = 0.08f)),
                Triple(0.9f, 0.85f, LudoYellow.copy(alpha = 0.1f)),
                Triple(0.15f, 0.9f, LudoBlue.copy(alpha = 0.08f)),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Row {
                Text("L", color = LudoRed, fontSize = 56.sp, fontWeight = FontWeight.Black)
                Text("U", color = LudoGreen, fontSize = 56.sp, fontWeight = FontWeight.Black)
                Text("D", color = LudoYellow, fontSize = 56.sp, fontWeight = FontWeight.Black)
                Text("O", color = LudoBlue, fontSize = 56.sp, fontWeight = FontWeight.Black)
            }

            Text(
                text = "Classic Board Game",
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Settings card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Player count
                    Text(
                        text = "Number of Players",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(2, 3, 4).forEach { count ->
                            val isSelected = playerCount == count
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) LudoGreen else Color.Transparent
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) LudoGreen else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { playerCount = count }
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Game mode
                    Text(
                        text = "Game Mode",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ModeButton(
                            text = "🤖 vs AI",
                            isSelected = isVsAI,
                            color = LudoBlue,
                            onClick = { isVsAI = true },
                            modifier = Modifier.weight(1f)
                        )
                        ModeButton(
                            text = "👥 Local",
                            isSelected = !isVsAI,
                            color = LudoRed,
                            onClick = { isVsAI = false },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // AI Difficulty
                    if (isVsAI) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "AI Difficulty",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ModeButton(
                                text = "😊 Easy",
                                isSelected = aiDifficulty == "Easy",
                                color = LudoGreen,
                                onClick = { aiDifficulty = "Easy" },
                                modifier = Modifier.weight(1f)
                            )
                            ModeButton(
                                text = "😈 Hard",
                                isSelected = aiDifficulty == "Hard",
                                color = LudoRed,
                                onClick = { aiDifficulty = "Hard" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Start button
            Button(
                onClick = { onStartGame(playerCount, isVsAI, aiDifficulty) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .scale(startScale),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LudoGreen
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "🎲  START GAME",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color(0xFFF5F5F5))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
