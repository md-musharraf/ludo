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
import com.example.ludo.theme.HairlineBorder

/**
 * One status dot per token:
 * - FINISHED: filled with the player colour.
 * - ON_BOARD / IN_HOME_COLUMN: ring in the player colour.
 * - IN_HOME: neutral grey.
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
        for (token in tokens) {
            val (fill, ring, ringWidth) = when (token.state) {
                TokenState.FINISHED -> Triple(playerColor, playerColor, 0.dp)
                TokenState.ON_BOARD, TokenState.IN_HOME_COLUMN -> Triple(Color.White, playerColor, 1.8.dp)
                TokenState.IN_HOME -> Triple(HairlineBorder, HairlineBorder, 0.dp)
            }
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(fill)
                    .border(ringWidth, ring, CircleShape)
            )
        }
    }
}
