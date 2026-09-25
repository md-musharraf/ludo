package com.example.ludo.ui.components

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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.model.Player

private val DockShape = RoundedCornerShape(14.dp)
private val IdleDockBrush = Brush.verticalGradient(listOf(Color.White, Color(0xFFFBFBFB)))

/** Which side of the dock the dice sits on, so dice cups hug the outer screen edges. */
enum class DiceSide { START, END }

@Composable
fun CornerPlayerDock(
    player: Player?,
    isCurrentTurn: Boolean,
    canRoll: Boolean,
    isRolling: Boolean,
    diceValue: Int,
    diceSide: DiceSide,
    onDiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (player == null) {
        Box(modifier = modifier)
        return
    }

    val playerColor = PlayerColorUtils.getComposeColor(player.color)
    val activeBrush = remember(player.color) {
        Brush.verticalGradient(listOf(Color.White, PlayerColorUtils.getLightColor(player.color).copy(alpha = 0.28f)))
    }
    val glow = rememberPulse(active = isCurrentTurn, from = 0.35f, to = 1f, durationMs = 850)
    val borderWidth = if (isCurrentTurn) 2.dp else 1.dp

    Row(
        modifier = modifier
            .shadow(
                elevation = if (isCurrentTurn) 6.dp else 1.5.dp,
                shape = DockShape,
                ambientColor = if (isCurrentTurn) playerColor.copy(alpha = 0.35f) else Color.Transparent,
                spotColor = if (isCurrentTurn) playerColor else Color.Transparent
            )
            .clip(DockShape)
            .background(if (isCurrentTurn) activeBrush else IdleDockBrush)
            .drawWithContent {
                drawContent()
                val stroke = borderWidth.toPx()
                drawRoundRect(
                    color = if (isCurrentTurn) playerColor.copy(alpha = glow.value) else Color(0xFFE5E0D8),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = stroke)
                )
            }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val dice = @Composable {
            DiceView(
                diceValue = diceValue,
                isRolling = isRolling,
                enabled = canRoll,
                playerColor = playerColor,
                onClick = onDiceClick
            )
        }
        if (diceSide == DiceSide.START) dice()
        PlayerInfoSection(player, playerColor, isCurrentTurn, Modifier.weight(1f))
        if (diceSide == DiceSide.END) dice()
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(playerColor.copy(alpha = 0.18f))
                    .border(1.2.dp, playerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (player.isAI) "🤖" else "👤", fontSize = 10.sp)
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

        if (player.hasFinished) {
            Text(
                text = rankLabel(player.rank),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = playerColor
            )
        } else {
            TokenIndicatorRow(tokens = player.tokens, playerColor = playerColor, dotSize = 8.dp, spacing = 3.dp)
        }
    }
}

fun rankLabel(rank: Int): String = when (rank) {
    1 -> "🥇 1st"
    2 -> "🥈 2nd"
    3 -> "🥉 3rd"
    else -> "${rank}th"
}
