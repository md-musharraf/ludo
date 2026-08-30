package com.example.ludo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ludo.model.Token
import com.example.ludo.model.TokenState
import com.example.ludo.theme.SafeZoneStar

/**
 * Reusable token status indicator row.
 * Displays 4 status badges corresponding to each token:
 * - FINISHED: Filled with player color + star border.
 * - ON_BOARD / IN_HOME_COLUMN: White interior with thick player color border.
 * - IN_HOME: Neutral muted grey indicator.
 */
@Composable
fun TokenIndicatorRow(
    tokens: List<Token>,
    playerColor: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    spacing: Dp = 3.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until tokens.size) {
            val token = tokens[i]
            when (token.state) {
                TokenState.FINISHED -> {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(playerColor)
                            .border(1.dp, SafeZoneStar, CircleShape)
                    )
                }
                TokenState.ON_BOARD, TokenState.IN_HOME_COLUMN -> {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.6.dp, playerColor, CircleShape)
                    )
                }
                TokenState.IN_HOME -> {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(Color(0xFFCFD8DC))
                    )
                }
            }
        }
    }
}
