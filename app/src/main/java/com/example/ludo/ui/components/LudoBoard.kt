package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ludo.engine.BoardConfig
import com.example.ludo.model.*
import com.example.ludo.theme.*
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private data class TokenRenderInfo(
    val playerId: Int,
    val tokenId: Int,
    val playerColor: PlayerColor,
    val center: Offset,
    val radius: Float,
    val isValid: Boolean,
    val isCurrentPlayer: Boolean,
    val isAnimating: Boolean = false,
    val shadowOffset: Offset = Offset(2.5f, 4f),
    val shadowRadius: Float = 1.0f
)

/**
 * Authentic Clean Ludo Board with iconic White & Jewel-Colored Map-Pin Gotis.
 * Exact replication of classic mobile board styling:
 * - 4 vibrant home bases with dynamic empty/occupied socket beds
 * - 4 center triangles meeting at center apex
 * - 4 safe-spot outlined stars
 * - 4 directional entry arrows
 * - Iconic teardrop / map-pin gotis with ground anchor rings and realistic drop shadows
 */
@Composable
fun LudoBoard(
    gameState: GameState,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "boardAnimations")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val renderedTokens = remember { mutableListOf<TokenRenderInfo>() }

    Canvas(
        modifier = modifier.pointerInput(gameState) {
            detectTapGestures { tapOffset ->
                val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex) ?: return@detectTapGestures

                val tokensCopy = synchronized(renderedTokens) { renderedTokens.toList() }
                for (info in tokensCopy.reversed()) {
                    if (info.playerId == currentPlayer.id && info.isValid) {
                        val dx = tapOffset.x - info.center.x
                        val dy = tapOffset.y - info.center.y
                        val hitDistance = info.radius * 2.4f
                        if (dx * dx + dy * dy <= hitDistance * hitDistance) {
                            onTokenClick(info.tokenId)
                            return@detectTapGestures
                        }
                    }
                }
            }
        }
    ) {
        val boardSize = min(size.width, size.height)
        val cellSize = boardSize / 15f
        val offsetX = (size.width - boardSize) / 2f
        val offsetY = (size.height - boardSize) / 2f

        val tokenList = mutableListOf<TokenRenderInfo>()
        val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex)

        // 1. Clean Crisp Board Canvas Background
        drawRoundRect(
            color = Color(0x28000000),
            topLeft = Offset(offsetX + 3f, offsetY + 5f),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.45f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.45f)
        )

        // 2. Four Classic Home Bases with dynamic empty socket fills
        drawHomeBase(
            offsetX, offsetY, cellSize, 0, 0, LudoRed,
            currentPlayer?.color == PlayerColor.RED, pulseAlpha,
            playerTokens = gameState.players.firstOrNull { it.color == PlayerColor.RED }?.tokens ?: emptyList()
        )
        drawHomeBase(
            offsetX, offsetY, cellSize, 0, 9, LudoGreen,
            currentPlayer?.color == PlayerColor.GREEN, pulseAlpha,
            playerTokens = gameState.players.firstOrNull { it.color == PlayerColor.GREEN }?.tokens ?: emptyList()
        )
        drawHomeBase(
            offsetX, offsetY, cellSize, 9, 9, LudoYellow,
            currentPlayer?.color == PlayerColor.YELLOW, pulseAlpha,
            playerTokens = gameState.players.firstOrNull { it.color == PlayerColor.YELLOW }?.tokens ?: emptyList()
        )
        drawHomeBase(
            offsetX, offsetY, cellSize, 9, 0, LudoBlue,
            currentPlayer?.color == PlayerColor.BLUE, pulseAlpha,
            playerTokens = gameState.players.firstOrNull { it.color == PlayerColor.BLUE }?.tokens ?: emptyList()
        )

        // 3. Track Cells (52 Squares)
        drawTrackCells(offsetX, offsetY, cellSize)

        // 4. Starting Squares
        drawStartingSquares(offsetX, offsetY, cellSize)

        // 5. Safe Star Zones (4 Stars with clean dark outlines and white interior)
        drawSafeZones(offsetX, offsetY, cellSize)

        // 6. Home Columns (Solid Player Colored Paths into Center)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.redHomeColumn, LudoRed)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.greenHomeColumn, LudoGreen)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.yellowHomeColumn, LudoYellow)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.blueHomeColumn, LudoBlue)

        // 7. Directional Arrows on Track
        drawTrackArrows(offsetX, offsetY, cellSize)

        // 8. Center Home Triangles (Clean 4 Triangles Meeting at Apex)
        drawCenterHome(offsetX, offsetY, cellSize)

        // 9. Outer Board Border Frame
        drawRoundRect(
            color = Color(0xFF212121),
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.45f),
            style = Stroke(width = 2.5f)
        )

        // 10. Collect Home Base Tokens
        for (player in gameState.players) {
            val colorOrdinal = player.color.ordinal
            for (token in player.tokens) {
                if (token.state == TokenState.IN_HOME) {
                    val homePos = BoardConfig.homePositions[colorOrdinal]?.getOrNull(token.id)
                    if (homePos != null) {
                        val cx = offsetX + homePos.second * cellSize + cellSize / 2
                        val cy = offsetY + homePos.first * cellSize + cellSize / 2
                        val isCurr = player.id == currentPlayer?.id
                        val isValid = isCurr && gameState.validMoves.contains(token.id) && gameState.gamePhase == GamePhase.WAITING_FOR_MOVE
                        val radius = cellSize * 0.44f

                        tokenList.add(
                            TokenRenderInfo(
                                playerId = player.id,
                                tokenId = token.id,
                                playerColor = player.color,
                                center = Offset(cx, cy),
                                radius = radius,
                                isValid = isValid,
                                isCurrentPlayer = isCurr
                            )
                        )
                    }
                }
            }
        }

        // 11. Collect Board Tokens with Stacking
        val boardTokensByCell = mutableMapOf<Pair<Int, Int>, MutableList<Pair<Player, Token>>>()
        for (player in gameState.players) {
            for (token in player.tokens) {
                if (token.state == TokenState.ON_BOARD || token.state == TokenState.IN_HOME_COLUMN) {
                    token.boardPosition?.let { pos ->
                        boardTokensByCell.getOrPut(pos) { mutableListOf() }.add(Pair(player, token))
                    }
                }
            }
        }

        for ((cellPos, tokensInCell) in boardTokensByCell) {
            val (row, col) = cellPos
            val cellCenterX = offsetX + col * cellSize + cellSize / 2
            val cellCenterY = offsetY + row * cellSize + cellSize / 2
            val count = tokensInCell.size

            tokensInCell.forEachIndexed { index, (player, token) ->
                if (gameState.animatingTokenId == token.id && gameState.animatingPlayerId == player.id && gameState.animatingFromPos != null) {
                    return@forEachIndexed
                }

                val (offsetXShift, offsetYShift, tokenRadius) = when (count) {
                    1 -> Triple(0f, 0f, cellSize * 0.44f)
                    2 -> {
                        val d = cellSize * 0.16f
                        if (index == 0) Triple(-d, -d, cellSize * 0.32f)
                        else Triple(d, d, cellSize * 0.32f)
                    }
                    3 -> {
                        val d = cellSize * 0.16f
                        when (index) {
                            0 -> Triple(0f, -d, cellSize * 0.28f)
                            1 -> Triple(-d, d * 0.9f, cellSize * 0.28f)
                            else -> Triple(d, d * 0.9f, cellSize * 0.28f)
                        }
                    }
                    else -> {
                        val d = cellSize * 0.18f
                        when (index % 4) {
                            0 -> Triple(-d, -d, cellSize * 0.24f)
                            1 -> Triple(d, -d, cellSize * 0.24f)
                            2 -> Triple(-d, d, cellSize * 0.24f)
                            else -> Triple(d, d, cellSize * 0.24f)
                        }
                    }
                }

                val cx = cellCenterX + offsetXShift
                val cy = cellCenterY + offsetYShift
                val isCurr = player.id == currentPlayer?.id
                val isValid = isCurr && gameState.validMoves.contains(token.id) && gameState.gamePhase == GamePhase.WAITING_FOR_MOVE

                tokenList.add(
                    TokenRenderInfo(
                        playerId = player.id,
                        tokenId = token.id,
                        playerColor = player.color,
                        center = Offset(cx, cy),
                        radius = tokenRadius,
                        isValid = isValid,
                        isCurrentPlayer = isCurr
                    )
                )
            }
        }

        // 12. Smooth Parabolic Hop Animation
        if (gameState.animatingPlayerId != null && gameState.animatingTokenId != null &&
            gameState.animatingFromPos != null && gameState.animatingToPos != null) {

            val from = gameState.animatingFromPos
            val to = gameState.animatingToPos
            val progress = gameState.animatingHopProgress

            val fromX = offsetX + from.second * cellSize + cellSize / 2
            val fromY = offsetY + from.first * cellSize + cellSize / 2
            val toX = offsetX + to.second * cellSize + cellSize / 2
            val toY = offsetY + to.first * cellSize + cellSize / 2

            val curX = fromX + (toX - fromX) * progress
            val curY = fromY + (toY - fromY) * progress
            val hopHeight = sin(progress * Math.PI).toFloat() * cellSize * 0.85f
            val elevatedY = curY - hopHeight
            val elevatedScale = 1f + sin(progress * Math.PI).toFloat() * 0.32f
            val tokenRadius = cellSize * 0.44f * elevatedScale

            val player = gameState.players.firstOrNull { it.id == gameState.animatingPlayerId }
            if (player != null) {
                tokenList.add(
                    TokenRenderInfo(
                        playerId = player.id,
                        tokenId = gameState.animatingTokenId,
                        playerColor = player.color,
                        center = Offset(curX, elevatedY),
                        radius = tokenRadius,
                        isValid = false,
                        isCurrentPlayer = true,
                        isAnimating = true,
                        shadowOffset = Offset(2.5f, hopHeight + 4f),
                        shadowRadius = 1f + hopHeight / 8f
                    )
                )
            }
        }

        // 13. Draw All Pin Gotis (Pawns)
        for (info in tokenList.sortedBy { if (it.isAnimating) 1 else 0 }) {
            drawPinGoti(
                center = info.center,
                radius = info.radius,
                color = getPlayerColor(info.playerColor),
                isValid = info.isValid,
                pulseAlpha = pulseAlpha,
                pulseScale = pulseScale,
                shadowOffset = info.shadowOffset,
                shadowRadius = info.shadowRadius,
                isAnimating = info.isAnimating
            )
        }

        synchronized(renderedTokens) {
            renderedTokens.clear()
            renderedTokens.addAll(tokenList)
        }
    }
}

/**
 * Renders the iconic White & Jewel-Colored Map-Pin Goti (Pawn).
 * Features:
 * - Base anchor ring on the cell with subtle shadow
 * - Soft drop shadow cast to bottom-right
 * - White teardrop / map-pin body with beveled outline
 * - Jewel colored circular core inside the pin head
 * - Specular gloss highlight
 * - Golden pulsing halo when valid to move
 */
private fun DrawScope.drawPinGoti(
    center: Offset,
    radius: Float,
    color: Color,
    isValid: Boolean,
    pulseAlpha: Float,
    pulseScale: Float,
    shadowOffset: Offset,
    shadowRadius: Float,
    isAnimating: Boolean
) {
    val (cx, cy) = center.x to center.y

    // 1. Pulsing Halo when Valid to Move
    if (isValid) {
        drawCircle(
            color = color.copy(alpha = pulseAlpha * 0.55f),
            radius = radius * pulseScale * 1.55f,
            center = Offset(cx, cy - radius * 0.15f)
        )
        drawCircle(
            color = Color.White.copy(alpha = pulseAlpha * 0.95f),
            radius = radius * pulseScale * 1.25f,
            center = Offset(cx, cy - radius * 0.15f),
            style = Stroke(width = 2.4f)
        )
    }

    // 2. Base Contact Ring on Board
    val groundCenter = Offset(cx, cy + radius * 0.42f)
    drawCircle(
        color = Color(0x33000000),
        radius = radius * 0.62f,
        center = Offset(groundCenter.x + 1f, groundCenter.y + 1.2f)
    )
    drawCircle(
        color = Color.White,
        radius = radius * 0.58f,
        center = groundCenter
    )
    drawCircle(
        color = color,
        radius = radius * 0.48f,
        center = groundCenter,
        style = Stroke(width = 2.4f)
    )

    // 3. Pin Geometry Definition
    val pinHeadRadius = radius * 0.62f
    val pinHeadCenter = Offset(cx, cy - radius * 0.28f)
    val pinTip = Offset(cx, cy + radius * 0.45f)

    fun createPinPath(headCenter: Offset, tip: Offset, r: Float): Path {
        val path = Path()
        val dy = tip.y - headCenter.y
        val clampedRatio = (r / dy).coerceIn(0f, 1f).toDouble()
        val angleDeg = Math.toDegrees(asin(clampedRatio)).toFloat()

        val leftTangentX = headCenter.x - r * cos(Math.toRadians((90.0 - angleDeg))).toFloat()
        val leftTangentY = headCenter.y + r * sin(Math.toRadians((90.0 - angleDeg))).toFloat()
        val rightTangentX = headCenter.x + r * cos(Math.toRadians((90.0 - angleDeg))).toFloat()
        val rightTangentY = headCenter.y + r * sin(Math.toRadians((90.0 - angleDeg))).toFloat()

        path.moveTo(tip.x, tip.y)
        path.lineTo(leftTangentX, leftTangentY)
        path.arcTo(
            rect = Rect(
                left = headCenter.x - r,
                top = headCenter.y - r,
                right = headCenter.x + r,
                bottom = headCenter.y + r
            ),
            startAngleDegrees = 180f - angleDeg,
            sweepAngleDegrees = 180f + 2f * angleDeg,
            forceMoveTo = false
        )
        path.lineTo(tip.x, tip.y)
        path.close()
        return path
    }

    // 4. Drop Shadow of the Pin
    val shadowPath = createPinPath(
        headCenter = Offset(pinHeadCenter.x + shadowOffset.x, pinHeadCenter.y + shadowOffset.y),
        tip = Offset(pinTip.x + shadowOffset.x, pinTip.y + shadowOffset.y),
        r = pinHeadRadius * shadowRadius
    )
    drawPath(shadowPath, color = Color(0x38000000), style = Fill)

    // 5. White Outer Pin Shell
    val pinPath = createPinPath(pinHeadCenter, pinTip, pinHeadRadius)
    drawPath(pinPath, color = Color.White, style = Fill)
    drawPath(pinPath, color = Color(0xFF757575), style = Stroke(width = 1.6f))

    // 6. Colored Inner Circle in Pin Head
    val innerColorRadius = pinHeadRadius * 0.65f
    drawCircle(
        color = color,
        radius = innerColorRadius,
        center = pinHeadCenter
    )
    drawCircle(
        color = Color(0x22000000),
        radius = innerColorRadius,
        center = pinHeadCenter,
        style = Stroke(width = 1.2f)
    )

    // 7. Specular Gloss Highlight
    val glossCenter = Offset(pinHeadCenter.x - innerColorRadius * 0.28f, pinHeadCenter.y - innerColorRadius * 0.28f)
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = innerColorRadius * 0.35f,
        center = glossCenter
    )
}

private fun DrawScope.drawHomeBase(
    offsetX: Float, offsetY: Float, cellSize: Float,
    startRow: Int, startCol: Int, color: Color,
    isCurrentPlayer: Boolean, pulseAlpha: Float,
    playerTokens: List<Token>
) {
    val x = offsetX + startCol * cellSize
    val y = offsetY + startRow * cellSize
    val homeSize = cellSize * 6

    // Outer Colored Rounded Frame
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(homeSize, homeSize),
        cornerRadius = CornerRadius(cellSize * 0.45f)
    )

    // Active Player Turn Halo
    if (isCurrentPlayer) {
        drawRoundRect(
            color = color.copy(alpha = pulseAlpha * 0.6f),
            topLeft = Offset(x - 3f, y - 3f),
            size = Size(homeSize + 6f, homeSize + 6f),
            cornerRadius = CornerRadius(cellSize * 0.45f),
            style = Stroke(width = 3.5f)
        )
    }

    // Inner White Platform
    val innerMargin = cellSize * 0.85f
    val innerSize = homeSize - innerMargin * 2
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(x + innerMargin, y + innerMargin),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(cellSize * 0.38f)
    )

    // 4 Sockets (Dynamic Empty vs Occupied Socket Appearance)
    val spotRadius = cellSize * 0.46f
    val spotPositions = listOf(
        0 to Offset(x + 1.5f * cellSize, y + 1.5f * cellSize),
        1 to Offset(x + 4.5f * cellSize, y + 1.5f * cellSize),
        2 to Offset(x + 1.5f * cellSize, y + 4.5f * cellSize),
        3 to Offset(x + 4.5f * cellSize, y + 4.5f * cellSize)
    )

    spotPositions.forEach { (tokenId, center) ->
        val isTokenInHome = playerTokens.firstOrNull { it.id == tokenId }?.state == TokenState.IN_HOME

        // Outer socket shadow
        drawCircle(color = Color(0x18000000), radius = spotRadius * 1.08f, center = Offset(center.x + 0.8f, center.y + 1f))
        // Colored socket rim
        drawCircle(color = color.copy(alpha = 0.25f), radius = spotRadius, center = center)
        drawCircle(color = color, radius = spotRadius, center = center, style = Stroke(width = 2.5f))

        if (isTokenInHome) {
            // White socket bed for occupied token
            drawCircle(color = Color.White, radius = spotRadius * 0.65f, center = center)
        } else {
            // Solid player color for empty socket (matches reference image)
            drawCircle(color = color, radius = spotRadius * 0.82f, center = center)
        }
    }
}

private fun DrawScope.drawTrackCells(offsetX: Float, offsetY: Float, cellSize: Float) {
    for (pos in BoardConfig.mainTrack) {
        val (row, col) = pos
        val x = offsetX + col * cellSize
        val y = offsetY + row * cellSize

        drawRect(
            color = Color.White,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color(0xFFCFD8DC),
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawStartingSquares(offsetX: Float, offsetY: Float, cellSize: Float) {
    val starts = listOf(
        Pair(BoardConfig.RED_START_INDEX, LudoRed),
        Pair(BoardConfig.GREEN_START_INDEX, LudoGreen),
        Pair(BoardConfig.YELLOW_START_INDEX, LudoYellow),
        Pair(BoardConfig.BLUE_START_INDEX, LudoBlue)
    )
    for ((index, color) in starts) {
        val (row, col) = BoardConfig.mainTrack[index]
        val x = offsetX + col * cellSize
        val y = offsetY + row * cellSize

        drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color(0xFFB0BEC5),
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1.2f)
        )
    }
}

private fun DrawScope.drawHomeColumn(
    offsetX: Float, offsetY: Float, cellSize: Float,
    positions: List<Pair<Int, Int>>, color: Color
) {
    positions.forEachIndexed { _, (row, col) ->
        val x = offsetX + col * cellSize
        val y = offsetY + row * cellSize

        drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.5f),
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawTrackArrows(offsetX: Float, offsetY: Float, cellSize: Float) {
    val arrows = listOf(
        Triple(Pair(7, 0), LudoRed, 0f),       // Red arrow pointing Right →
        Triple(Pair(0, 7), LudoGreen, 90f),    // Green arrow pointing Down ↓
        Triple(Pair(7, 14), LudoYellow, 180f), // Yellow arrow pointing Left ←
        Triple(Pair(14, 7), LudoBlue, 270f)    // Blue arrow pointing Up ↑
    )

    arrows.forEach { (pos, color, angleDeg) ->
        val (row, col) = pos
        val cx = offsetX + col * cellSize + cellSize / 2
        val cy = offsetY + row * cellSize + cellSize / 2

        drawDirectionArrow(cx, cy, cellSize * 0.45f, color, angleDeg)
    }
}

private fun DrawScope.drawDirectionArrow(cx: Float, cy: Float, size: Float, color: Color, angleDeg: Float) {
    val path = Path()
    val half = size / 2

    // Arrow pointing Right (0 deg)
    val tipX = half
    val tipY = 0f
    val backX = -half
    val topY = -half * 0.7f
    val botY = half * 0.7f

    val rad = Math.toRadians(angleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    fun rotatePoint(px: Float, py: Float): Offset {
        val rx = px * cosA - py * sinA
        val ry = px * sinA + py * cosA
        return Offset(cx + rx, cy + ry)
    }

    val pTip = rotatePoint(tipX, tipY)
    val pTop = rotatePoint(backX, topY)
    val pBot = rotatePoint(backX, botY)
    val pMid = rotatePoint(backX * 0.4f, 0f)

    path.moveTo(pTip.x, pTip.y)
    path.lineTo(pTop.x, pTop.y)
    path.lineTo(pMid.x, pMid.y)
    path.lineTo(pBot.x, pBot.y)
    path.close()

    drawPath(path, color = color, style = Fill)
}

private fun DrawScope.drawSafeZones(offsetX: Float, offsetY: Float, cellSize: Float) {
    // 4 Classic Safe Spot Stars (Outlined Star with White Interior matching reference)
    val starIndices = listOf(8, 21, 34, 47)
    for (index in starIndices) {
        val (row, col) = BoardConfig.mainTrack[index]
        val cx = offsetX + col * cellSize + cellSize / 2
        val cy = offsetY + row * cellSize + cellSize / 2

        drawStar(cx, cy, cellSize * 0.36f, Color.White, Color(0xFF424242))
    }
}

private fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, fillColor: Color, strokeColor: Color) {
    val points = 5
    val path = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.42f
        val angle = Math.toRadians((i * 360.0 / (points * 2)) - 90.0)
        val x = cx + r * cos(angle).toFloat()
        val y = cy + r * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = fillColor, style = Fill)
    drawPath(path, color = strokeColor, style = Stroke(width = 1.8f))
}

private fun DrawScope.drawCenterHome(offsetX: Float, offsetY: Float, cellSize: Float) {
    val centerX = offsetX + 7.5f * cellSize
    val centerY = offsetY + 7.5f * cellSize
    val triangleSpan = cellSize * 1.5f

    val triangleDefs = listOf(
        Pair(
            listOf(Offset(centerX - triangleSpan, centerY - triangleSpan),
                   Offset(centerX, centerY),
                   Offset(centerX - triangleSpan, centerY + triangleSpan)),
            LudoRed
        ),
        Pair(
            listOf(Offset(centerX - triangleSpan, centerY - triangleSpan),
                   Offset(centerX + triangleSpan, centerY - triangleSpan),
                   Offset(centerX, centerY)),
            LudoGreen
        ),
        Pair(
            listOf(Offset(centerX + triangleSpan, centerY - triangleSpan),
                   Offset(centerX + triangleSpan, centerY + triangleSpan),
                   Offset(centerX, centerY)),
            LudoYellow
        ),
        Pair(
            listOf(Offset(centerX - triangleSpan, centerY + triangleSpan),
                   Offset(centerX + triangleSpan, centerY + triangleSpan),
                   Offset(centerX, centerY)),
            LudoBlue
        )
    )

    for ((points, color) in triangleDefs) {
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[1].x, points[1].y)
            lineTo(points[2].x, points[2].y)
            close()
        }
        drawPath(path, color = color, style = Fill)
    }
}

private fun getPlayerColor(playerColor: PlayerColor): Color {
    return when (playerColor) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
}


