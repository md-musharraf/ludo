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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.model.Player
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
        Box(modifier = modifier)
        return
    }

    val playerColor = PlayerColorUtils.getComposeColor(player.color)
    val playerLightColor = PlayerColorUtils.getLightColor(player.color)

    val infiniteTransition = rememberInfiniteTransition(label = "dockPulse")
    val glowAlpha by if (isCurrentTurn) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0.35f) }
    }

    val isLeft = corner == DockCorner.TOP_LEFT || corner == DockCorner.BOTTOM_LEFT

    Row(
        modifier = modifier
            .shadow(
                elevation = if (isCurrentTurn) 6.dp else 1.5.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = if (isCurrentTurn) playerColor.copy(alpha = 0.35f) else Color.Transparent,
                spotColor = if (isCurrentTurn) playerColor else Color.Transparent
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isCurrentTurn) listOf(
                        Color.White,
                        playerLightColor.copy(alpha = 0.28f)
                    ) else listOf(
                        Color.White,
                        Color(0xFFFBFBFB)
                    )
                )
            )
            .border(
                width = if (isCurrentTurn) 2.dp else 1.dp,
                color = if (isCurrentTurn) playerColor.copy(alpha = glowAlpha) else Color(0xFFE5E0D8),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isLeft) {
            DiceView(
                diceValue = diceValue,
                isRolling = isRolling,
                enabled = isCurrentTurn && !player.isAI,
                playerColor = playerColor,
                onClick = onDiceClick
            )
            PlayerInfoSection(
                player = player,
                playerColor = playerColor,
                isCurrentTurn = isCurrentTurn,
                modifier = Modifier.weight(1f)
            )
        } else {
            PlayerInfoSection(
                player = player,
                playerColor = playerColor,
                isCurrentTurn = isCurrentTurn,
                modifier = Modifier.weight(1f)
            )
            DiceView(
                diceValue = diceValue,
                isRolling = isRolling,
                enabled = isCurrentTurn && !player.isAI,
                playerColor = playerColor,
                onClick = onDiceClick
            )
        }
    }
}

@Composable
private fun PlayerInfoSection(
    player: Player,
    playerColor: Color,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(playerColor.copy(alpha = 0.18f))
                    .border(1.2.dp, playerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (player.isAI) "\uD83E\uDD16" else "\uD83D\uDC64",
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = player.name,
                fontSize = 11.sp,
                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrentTurn) playerColor else Color(0xFF424242),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        TokenIndicatorRow(
            tokens = player.tokens,
            playerColor = playerColor,
            dotSize = 8.dp,
            spacing = 3.dp
        )
    }
}

