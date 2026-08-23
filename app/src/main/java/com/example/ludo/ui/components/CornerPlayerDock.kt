package com.example.ludo.ui.components

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.model.Player
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.TokenState
import com.example.ludo.theme.*

enum class DockCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
}

@Composable
fun CornerPlayerDock(
    player: Player?,
    isCurrentTurn: Boolean,
    isRolling: Boolean,
    diceValue: Int,
    corner: DockCorner,
    onDiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (player == null) {
        Box(modifier = modifier.size(100.dp, 60.dp))
        return
    }

    val playerColor = getPlayerComposeColor(player.color)
    val playerLightColor = getPlayerLightColor(player.color)

    val infiniteTransition = rememberInfiniteTransition(label = "dockPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val isLeft = corner == DockCorner.TOP_LEFT || corner == DockCorner.BOTTOM_LEFT

    Row(
        modifier = modifier
            .scale(if (isCurrentTurn) pulseScale else 1f)
            .shadow(
                elevation = if (isCurrentTurn) 8.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isCurrentTurn) playerColor.copy(alpha = 0.4f) else Color.Transparent,
                spotColor = if (isCurrentTurn) playerColor else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isCurrentTurn) listOf(
                        Color.White,
                        playerLightColor.copy(alpha = 0.35f)
                    ) else listOf(
                        Color.White,
                        Color(0xFFF9F9F9)
                    )
                )
            )
            .border(
                width = if (isCurrentTurn) 2.dp else 1.dp,
                color = if (isCurrentTurn) playerColor.copy(alpha = glowAlpha) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLeft) {
            DiceView(
                diceValue = diceValue,
                isRolling = isRolling,
                enabled = isCurrentTurn && !player.isAI,
                playerColor = playerColor,
                showPromptBadge = true,
                onClick = onDiceClick
            )
            PlayerInfoSection(
                player = player,
                playerColor = playerColor,
                isCurrentTurn = isCurrentTurn
            )
        } else {
            PlayerInfoSection(
                player = player,
                playerColor = playerColor,
                isCurrentTurn = isCurrentTurn
            )
            DiceView(
                diceValue = diceValue,
                isRolling = isRolling,
                enabled = isCurrentTurn && !player.isAI,
                playerColor = playerColor,
                showPromptBadge = true,
                onClick = onDiceClick
            )
        }
    }
}

@Composable
private fun PlayerInfoSection(
    player: Player,
    playerColor: Color,
    isCurrentTurn: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(playerColor.copy(alpha = 0.2f))
                    .border(1.5.dp, playerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (player.isAI) "\uD83E\uDD16" else "\uD83D\uDC64",
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = player.name,
                fontSize = 12.sp,
                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrentTurn) playerColor else Color(0xFF424242),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            player.tokens.forEach { tok ->
                when (tok.state) {
                    TokenState.FINISHED -> {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(playerColor)
                                .border(1.dp, SafeZoneStar, CircleShape)
                        )
                    }
                    TokenState.ON_BOARD, TokenState.IN_HOME_COLUMN -> {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, playerColor, CircleShape)
                        )
                    }
                    TokenState.IN_HOME -> {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFBDBDBD))
                        )
                    }
                }
            }
        }
    }
}

private fun getPlayerComposeColor(color: PlayerColor): Color {
    return when (color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
}

private fun getPlayerLightColor(color: PlayerColor): Color {
    return when (color) {
        PlayerColor.RED -> LudoRedLight
        PlayerColor.GREEN -> LudoGreenLight
        PlayerColor.YELLOW -> LudoYellowLight
        PlayerColor.BLUE -> LudoBlueLight
    }
}
