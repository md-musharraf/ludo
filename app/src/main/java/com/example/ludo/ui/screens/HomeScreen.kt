package com.example.ludo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.audio.SoundEffectManager
import com.example.ludo.model.AiDifficulty
import com.example.ludo.theme.*
import com.example.ludo.ui.components.LudoTitle

@Composable
fun HomeScreen(onStartGame: (Int, Boolean, AiDifficulty) -> Unit) {
    // Saveable so choices survive rotation and returning from a match.
    var playerCount by rememberSaveable { mutableIntStateOf(4) }
    var isVsAI by rememberSaveable { mutableStateOf(true) }
    var aiDifficulty by rememberSaveable { mutableStateOf(AiDifficulty.HARD) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackgroundBrush)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isCompact = maxHeight < 680.dp
        val horizontalPadding = if (isCompact) 16.dp else 24.dp
        val titleSize = if (isCompact) 44.sp else 54.sp
        val titleSpacer = if (isCompact) 18.dp else 32.dp

        // Scrolls on short (landscape) screens, stays centred on tall ones, capped width on tablets.
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LudoTitle(fontSize = titleSize)

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "SUPER LUDO MASTER",
                fontSize = if (isCompact) 11.sp else 13.sp,
                color = TextBrown,
                fontWeight = FontWeight.Bold,
                letterSpacing = if (isCompact) 3.sp else 4.sp
            )

            Spacer(modifier = Modifier.height(titleSpacer))

            // Settings card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(if (isCompact) 14.dp else 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Number of Players",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isCompact) 14.sp else 16.sp,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp)
                    ) {
                        listOf(2, 3, 4).forEach { count ->
                            val isSelected = playerCount == count
                            val circleSize = if (isCompact) 48.dp else 56.dp
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(circleSize)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) LudoGreen.copy(alpha = 0.15f) else Color(0xFFF5F5F5)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) LudoGreen else Color(0xFFE0E0E0),
                                        shape = CircleShape
                                    )
                                    .selectable(selected = isSelected, role = Role.RadioButton) {
                                        SoundEffectManager.playButtonTap()
                                        playerCount = count
                                    }
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = if (isCompact) 18.sp else 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) LudoGreen else Color(0xFF757575)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompact) 14.dp else 22.dp))

                    Text(
                        text = "Game Mode",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isCompact) 14.sp else 16.sp,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            color = LudoGreen,
                            onClick = { isVsAI = false },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isVsAI) {
                        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 18.dp))

                        Text(
                            text = "AI Difficulty",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (isCompact) 14.sp else 16.sp,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ModeButton(
                                text = "😊 Easy",
                                isSelected = aiDifficulty == AiDifficulty.EASY,
                                color = LudoGreen,
                                onClick = { aiDifficulty = AiDifficulty.EASY },
                                modifier = Modifier.weight(1f)
                            )
                            ModeButton(
                                text = "😈 Hard",
                                isSelected = aiDifficulty == AiDifficulty.HARD,
                                color = LudoRed,
                                onClick = { aiDifficulty = AiDifficulty.HARD },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 18.dp else 28.dp))

            // Start button
            Button(
                onClick = {
                    SoundEffectManager.playButtonTap()
                    onStartGame(playerCount, isVsAI, aiDifficulty)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 50.dp else 58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LudoGreen
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "🎲  START GAME",
                    fontSize = if (isCompact) 18.sp else 20.sp,
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
                color = if (isSelected) color else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            )
            .selectable(selected = isSelected, role = Role.RadioButton) {
                SoundEffectManager.playButtonTap()
                onClick()
            }
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else Color(0xFF757575),
            textAlign = TextAlign.Center
        )
    }
}
