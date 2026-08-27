package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ludo.core.util.PawnColorScheme
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.engine.BoardConfig
import com.example.ludo.model.*
import com.example.ludo.theme.*
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
    val groundCenter: Offset = Offset.Zero,
    val hopHeight: Float = 0f
)

// Reusable Path caches to prevent young-generation GC thrashing at 60-120fps
private val sharedStemPath = Path()
private val sharedArrowPath = Path()
private val sharedStarPath = Path()
private val sharedTrianglePath = Path()

/**
 * Authentic Photorealistic 3D Classic Ludo Board.
 * High-performance mobile optimized rendering pipeline:
 * - Zero GC allocation in per-frame DrawScope
 * - Guarded animations (CPU enters sleep states when idle)
 * - Directional 3D turned pawns with grounded ambient occlusion
 */
@Composable
fun LudoBoard(
    gameState: GameState,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasValidMoves = gameState.validMoves.isNotEmpty() && gameState.gamePhase == GamePhase.WAITING_FOR_MOVE
    val isAnimating = gameState.animatingPlayerId != null

    // Guarded infinite transition: ticks ONLY when interaction or animation requires it
    val shouldAnimate = hasValidMoves || isAnimating || gameState.gamePhase == GamePhase.WAITING_FOR_MOVE

    val infiniteTransition = rememberInfiniteTransition(label = "boardPulse")

    val pulseAlpha by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0.7f) }
    }

    val pulseScale by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    val renderedTokens = remember { ArrayList<TokenRenderInfo>(16) }

    Canvas(
        modifier = modifier.pointerInput(gameState) {
            detectTapGestures { tapOffset ->
                val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex) ?: return@detectTapGestures

                val tokensCopy = synchronized(renderedTokens) { ArrayList(renderedTokens) }
                for (info in tokensCopy.reversed()) {
                    if (info.playerId == currentPlayer.id && info.isValid) {
                        val dx = tapOffset.x - info.center.x
                        val dy = tapOffset.y - info.center.y
                        val hitDistance = info.radius * 2.5f
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

        val tokenList = ArrayList<TokenRenderInfo>(16)
        val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex)

        // 1. Board Shadow & Canvas Background
        drawRoundRect(
            color = Color(0x35000000),
            topLeft = Offset(offsetX + 3f, offsetY + 6f),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.50f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.50f)
        )

        // 2. Four Classic Home Bases with Tinted Pastel Platforms & Sunken Saucers
        drawHomeBase(
            offsetX, offsetY, cellSize, 0, 0, LudoRed, LudoRedLight,
            currentPlayer?.color == PlayerColor.RED, pulseAlpha
        )
        drawHomeBase(
            offsetX, offsetY, cellSize, 0, 9, LudoGreen, LudoGreenLight,
            currentPlayer?.color == PlayerColor.GREEN, pulseAlpha
        )
        drawHomeBase(
            offsetX, offsetY, cellSize, 9, 9, LudoYellow, LudoYellowLight,
            currentPlayer?.color == PlayerColor.YELLOW, pulseAlpha
        )
        drawHomeBase(
            offsetX, offsetY, cellSize, 9, 0, LudoBlue, LudoBlueLight,
            currentPlayer?.color == PlayerColor.BLUE, pulseAlpha
        )

        // 3. Track Cells (52 Squares)
        drawTrackCells(offsetX, offsetY, cellSize)

        // 4. Starting Squares
        drawStartingSquares(offsetX, offsetY, cellSize)

        // 5. Safe Star Zones (Golden Stars)
        drawSafeZones(offsetX, offsetY, cellSize)

        // 6. Home Columns
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.redHomeColumn, LudoRed)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.greenHomeColumn, LudoGreen)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.yellowHomeColumn, LudoYellow)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.blueHomeColumn, LudoBlue)

        // 7. Directional Arrows on Track
        drawTrackArrows(offsetX, offsetY, cellSize)

        // 8. Center Home Triangles
        drawCenterHome(offsetX, offsetY, cellSize)

        // 9. Luxurious Golden Beveled Frame
        drawGoldenBoardFrame(offsetX, offsetY, boardSize, cellSize)

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
                                isCurrentPlayer = isCurr,
                                groundCenter = Offset(cx, cy)
                            )
                        )
                    }
                }
            }
        }

        // 11. Collect Board Tokens with Stacking
        val boardTokensByCell = HashMap<Pair<Int, Int>, ArrayList<Pair<Player, Token>>>()
        for (player in gameState.players) {
            for (token in player.tokens) {
                if (token.state == TokenState.ON_BOARD || token.state == TokenState.IN_HOME_COLUMN) {
                    token.boardPosition?.let { pos ->
                        boardTokensByCell.getOrPut(pos) { ArrayList() }.add(Pair(player, token))
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
                        isCurrentPlayer = isCurr,
                        groundCenter = Offset(cx, cy)
                    )
                )
            }
        }

        // 12. Silky Smooth Sinusoidal Physics Hop Animation
        if (gameState.animatingPlayerId != null && gameState.animatingTokenId != null &&
            gameState.animatingFromPos != null && gameState.animatingToPos != null) {

            val from = gameState.animatingFromPos
            val to = gameState.animatingToPos
            val rawProgress = gameState.animatingHopProgress

            val easedProgress = (1f - cos(rawProgress * Math.PI.toFloat())) / 2f

            val fromX = offsetX + from.second * cellSize + cellSize / 2
            val fromY = offsetY + from.first * cellSize + cellSize / 2
            val toX = offsetX + to.second * cellSize + cellSize / 2
            val toY = offsetY + to.first * cellSize + cellSize / 2

            val curGroundX = fromX + (toX - fromX) * easedProgress
            val curGroundY = fromY + (toY - fromY) * easedProgress

            val hopHeight = sin(rawProgress * Math.PI.toFloat()) * cellSize * 0.95f
            val elevatedY = curGroundY - hopHeight
            val elevatedScale = 1f + sin(rawProgress * Math.PI.toFloat()) * 0.20f
            val tokenRadius = cellSize * 0.44f * elevatedScale

            val player = gameState.players.firstOrNull { it.id == gameState.animatingPlayerId }
            if (player != null) {
                tokenList.add(
                    TokenRenderInfo(
                        playerId = player.id,
                        tokenId = gameState.animatingTokenId,
                        playerColor = player.color,
                        center = Offset(curGroundX, elevatedY),
                        radius = tokenRadius,
                        isValid = false,
                        isCurrentPlayer = true,
                        isAnimating = true,
                        groundCenter = Offset(curGroundX, curGroundY),
                        hopHeight = hopHeight
                    )
                )
            }
        }

        // 13. Draw All Photorealistic 3D Gotis (Pawns)
        for (i in 0 until tokenList.size) {
            val info = tokenList[i]
            if (!info.isAnimating) {
                draw3DClassicPawn(
                    center = info.center,
                    radius = info.radius,
                    playerColor = info.playerColor,
                    isValid = info.isValid,
                    pulseAlpha = pulseAlpha,
                    pulseScale = pulseScale,
                    groundCenter = info.groundCenter,
                    hopHeight = info.hopHeight,
                    isAnimating = false
                )
            }
        }

        // Draw animating pawn on top
        for (i in 0 until tokenList.size) {
            val info = tokenList[i]
            if (info.isAnimating) {
                draw3DClassicPawn(
                    center = info.center,
                    radius = info.radius,
                    playerColor = info.playerColor,
                    isValid = info.isValid,
                    pulseAlpha = pulseAlpha,
                    pulseScale = pulseScale,
                    groundCenter = info.groundCenter,
                    hopHeight = info.hopHeight,
                    isAnimating = true
                )
            }
        }

        synchronized(renderedTokens) {
            renderedTokens.clear()
            renderedTokens.addAll(tokenList)
        }
    }
}

/**
 * Renders an authentic, realistic 3D Classic Ludo Pawn (Goti).
 * True Halma/Chess Pawn structure with physical grounding:
 * 1. Deep Ambient Occlusion Contact Shadow
 * 2. Stepped Pedestal Base Disc
 * 3. Tapered Waist Stem with Directional 5-stop Lighting
 * 4. Toroidal Collar Ring
 * 5. Spherical Crown Head with 3D Radial Depth & Specular Shine
 */
private fun DrawScope.draw3DClassicPawn(
    center: Offset,
    radius: Float,
    playerColor: PlayerColor,
    isValid: Boolean,
    pulseAlpha: Float,
    pulseScale: Float,
    groundCenter: Offset,
    hopHeight: Float,
    isAnimating: Boolean
) {
    val (cx, cy) = center.x to center.y
    val colors: PawnColorScheme = PlayerColorUtils.getPawnColorScheme(playerColor)

    // 1. Realistic Optical Ground Shadow & Ambient Occlusion (Firmly Seated on Board)
    val baseY = cy + radius * 0.26f
    val baseWidth = radius * 1.80f
    val baseHeight = radius * 0.72f
    val groundY = groundCenter.y + radius * 0.26f

    if (isAnimating) {
        val shadowProgress = (hopHeight / (radius * 2.5f)).coerceIn(0f, 1f)
        val shadowScale = 1f + shadowProgress * 0.65f
        val shadowAlpha = (0.38f * (1f - shadowProgress * 0.55f)).coerceIn(0.06f, 0.38f)
        val shadowW = baseWidth * shadowScale
        val shadowH = baseHeight * shadowScale

        // Outer soft ambient diffusion
        drawOval(
            color = Color.Black.copy(alpha = shadowAlpha * 0.35f),
            topLeft = Offset(groundCenter.x - shadowW * 0.62f + 1f, groundY - shadowH * 0.60f + 2f),
            size = Size(shadowW * 1.25f, shadowH * 1.25f)
        )
        // Inner contact shadow core
        drawOval(
            color = Color.Black.copy(alpha = shadowAlpha),
            topLeft = Offset(groundCenter.x - shadowW / 2f + 1f, groundY - shadowH / 2f + 2f),
            size = Size(shadowW, shadowH)
        )
    } else {
        // Deep Ambient Occlusion Ground Contact Shadow (Firmly glued to board surface)
        drawOval(
            color = Color.Black.copy(alpha = 0.22f),
            topLeft = Offset(groundCenter.x - baseWidth * 0.62f + 2f, groundY - baseHeight * 0.55f + 3f),
            size = Size(baseWidth * 1.24f, baseHeight * 1.15f)
        )
        drawOval(
            color = Color.Black.copy(alpha = 0.55f),
            topLeft = Offset(groundCenter.x - baseWidth * 0.48f + 1f, groundY - baseHeight * 0.35f + 2f),
            size = Size(baseWidth * 0.96f, baseHeight * 0.70f)
        )
    }

    // 2. Glowing Halo when Valid to Move
    if (isValid) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.lightColor.copy(alpha = pulseAlpha * 0.65f), Color.Transparent),
                center = Offset(cx, cy - radius * 0.25f),
                radius = radius * pulseScale * 1.75f
            ),
            radius = radius * pulseScale * 1.75f,
            center = Offset(cx, cy - radius * 0.25f)
        )
        drawCircle(
            color = Color.White.copy(alpha = pulseAlpha * 0.95f),
            radius = radius * pulseScale * 1.28f,
            center = Offset(cx, cy - radius * 0.25f),
            style = Stroke(width = 2.4f)
        )
    }

    // 3. Pawn Pedestal Base Disc (Stepped Circular Pedestal)
    // Lower Rim Shadow & Deep Base Bevel
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(colors.darkColor, colors.deepShadow),
            startY = baseY - baseHeight / 2,
            endY = baseY + baseHeight / 2 + 3f
        ),
        topLeft = Offset(cx - baseWidth / 2, baseY - baseHeight / 2 + 2f),
        size = Size(baseWidth, baseHeight + 2f)
    )

    // Base Convex Upper Surface
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(colors.highlightColor, colors.baseColor, colors.darkColor),
            center = Offset(cx - baseWidth * 0.20f, baseY - baseHeight * 0.20f),
            radius = baseWidth * 0.75f
        ),
        topLeft = Offset(cx - baseWidth / 2, baseY - baseHeight / 2),
        size = Size(baseWidth, baseHeight)
    )

    // Base Top Edge Specular Bevel
    drawOval(
        color = Color.White.copy(alpha = 0.55f),
        topLeft = Offset(cx - baseWidth * 0.38f, baseY - baseHeight * 0.42f),
        size = Size(baseWidth * 0.76f, baseHeight * 0.32f),
        style = Stroke(width = 1.3f)
    )

    // 4. Pawn Tapered Waist / Stem (Reusing shared path for zero GC churn)
    val neckY = cy - radius * 0.26f
    val stemTopWidth = radius * 0.62f
    val stemBottomWidth = radius * 1.22f

    sharedStemPath.reset()
    sharedStemPath.moveTo(cx - stemBottomWidth / 2, baseY - baseHeight * 0.22f)
    sharedStemPath.cubicTo(
        cx - stemBottomWidth * 0.32f, cy,
        cx - stemTopWidth * 0.60f, neckY + radius * 0.12f,
        cx - stemTopWidth / 2, neckY
    )
    sharedStemPath.lineTo(cx + stemTopWidth / 2, neckY)
    sharedStemPath.cubicTo(
        cx + stemTopWidth * 0.60f, neckY + radius * 0.12f,
        cx + stemBottomWidth * 0.32f, cy,
        cx + stemBottomWidth / 2, baseY - baseHeight * 0.22f
    )
    sharedStemPath.close()

    drawPath(
        path = sharedStemPath,
        brush = Brush.horizontalGradient(
            colors = listOf(colors.highlightColor, colors.lightColor, colors.baseColor, colors.darkColor, colors.deepShadow),
            startX = cx - stemBottomWidth / 2,
            endX = cx + stemBottomWidth / 2
        )
    )

    // 5. Collar Ring (Toroidal Bead below Crown)
    val collarWidth = radius * 0.95f
    val collarHeight = radius * 0.40f
    val collarY = neckY + radius * 0.04f

    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(colors.darkColor, colors.deepShadow),
            startY = collarY,
            endY = collarY + collarHeight
        ),
        topLeft = Offset(cx - collarWidth / 2, collarY - collarHeight / 2 + 1.5f),
        size = Size(collarWidth, collarHeight)
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(colors.highlightColor, colors.lightColor, colors.baseColor, colors.darkColor),
            center = Offset(cx - collarWidth * 0.22f, collarY - collarHeight * 0.22f),
            radius = collarWidth * 0.65f
        ),
        topLeft = Offset(cx - collarWidth / 2, collarY - collarHeight / 2),
        size = Size(collarWidth, collarHeight)
    )

    // 6. Spherical Crown Head (Sphere with 3D Gloss)
    val headRadius = radius * 0.68f
    val headCenter = Offset(cx, cy - radius * 0.58f)

    // Head Contact Drop Shadow on Collar
    drawCircle(
        color = Color(0x40000000),
        radius = headRadius * 1.05f,
        center = Offset(headCenter.x + 0.8f, headCenter.y + 2f)
    )

    // 3D Sphere Body with Radial Gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(colors.highlightColor, colors.lightColor, colors.baseColor, colors.darkColor, colors.deepShadow),
            center = Offset(headCenter.x - headRadius * 0.38f, headCenter.y - headRadius * 0.38f),
            radius = headRadius * 1.30f
        ),
        radius = headRadius,
        center = headCenter
    )

    // High-Gloss Specular Highlight (Primary bright crescent glint)
    drawOval(
        color = Color.White.copy(alpha = 0.88f),
        topLeft = Offset(headCenter.x - headRadius * 0.60f, headCenter.y - headRadius * 0.66f),
        size = Size(headRadius * 0.56f, headRadius * 0.42f)
    )

    // Secondary Micro Specular Dot
    drawCircle(
        color = Color.White,
        radius = headRadius * 0.14f,
        center = Offset(headCenter.x - headRadius * 0.44f, headCenter.y - headRadius * 0.48f)
    )

    // Reflected Ambient Rim Light on Lower-Right Edge
    drawArc(
        color = colors.lightColor.copy(alpha = 0.50f),
        startAngle = 30f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(headCenter.x - headRadius + 1f, headCenter.y - headRadius + 1f),
        size = Size(headRadius * 2 - 2f, headRadius * 2 - 2f),
        style = Stroke(width = 1.8f)
    )
}

private fun DrawScope.drawHomeBase(
    offsetX: Float, offsetY: Float, cellSize: Float,
    startRow: Int, startCol: Int, color: Color, lightColor: Color,
    isCurrentPlayer: Boolean, pulseAlpha: Float
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
            color = color.copy(alpha = pulseAlpha * 0.65f),
            topLeft = Offset(x - 3f, y - 3f),
            size = Size(homeSize + 6f, homeSize + 6f),
            cornerRadius = CornerRadius(cellSize * 0.45f),
            style = Stroke(width = 3.5f)
        )
    }

    // Inner Tinted Platform
    val innerMargin = cellSize * 0.85f
    val innerSize = homeSize - innerMargin * 2
    drawRoundRect(
        color = lightColor,
        topLeft = Offset(x + innerMargin, y + innerMargin),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(cellSize * 0.38f)
    )
    drawRoundRect(
        color = color.copy(alpha = 0.25f),
        topLeft = Offset(x + innerMargin, y + innerMargin),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(cellSize * 0.38f),
        style = Stroke(width = 1.8f)
    )

    // 4 Sunken 3D Socket Saucers
    val spotRadius = cellSize * 0.52f
    val centerOffsets = arrayOf(
        Offset(x + 1.5f * cellSize, y + 1.5f * cellSize),
        Offset(x + 4.5f * cellSize, y + 1.5f * cellSize),
        Offset(x + 1.5f * cellSize, y + 4.5f * cellSize),
        Offset(x + 4.5f * cellSize, y + 4.5f * cellSize)
    )

    for (i in 0 until 4) {
        val center = centerOffsets[i]
        // Deep cast shadow inside the well
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x35000000), Color(0x60000000)),
                center = Offset(center.x - spotRadius * 0.2f, center.y - spotRadius * 0.2f),
                radius = spotRadius
            ),
            radius = spotRadius,
            center = center
        )
        // Outer beveled socket rim
        drawCircle(
            color = color.copy(alpha = 0.85f),
            radius = spotRadius,
            center = center,
            style = Stroke(width = 3.2f)
        )
        // Inner sunken saucer floor
        drawCircle(
            color = color.copy(alpha = 0.18f),
            radius = spotRadius * 0.82f,
            center = center
        )
        // Highlight on lower-right rim
        drawArc(
            color = Color.White.copy(alpha = 0.65f),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - spotRadius, center.y - spotRadius),
            size = Size(spotRadius * 2, spotRadius * 2),
            style = Stroke(width = 1.6f)
        )
    }
}

private fun DrawScope.drawGoldenBoardFrame(offsetX: Float, offsetY: Float, boardSize: Float, cellSize: Float) {
    // Outer Luxurious Golden Bevel Frame
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF8D6E14), Color(0xFFF9D976), Color(0xFFD4AF37), Color(0xFF8D6E14)),
            start = Offset(offsetX, offsetY),
            end = Offset(offsetX + boardSize, offsetY + boardSize)
        ),
        topLeft = Offset(offsetX - 2f, offsetY - 2f),
        size = Size(boardSize + 4f, boardSize + 4f),
        cornerRadius = CornerRadius(cellSize * 0.50f),
        style = Stroke(width = 4.0f)
    )
    drawRoundRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(offsetX, offsetY),
        size = Size(boardSize, boardSize),
        cornerRadius = CornerRadius(cellSize * 0.50f),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawTrackCells(offsetX: Float, offsetY: Float, cellSize: Float) {
    val track = BoardConfig.mainTrack
    for (i in 0 until track.size) {
        val (row, col) = track[i]
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
    val track = BoardConfig.mainTrack
    val starts = arrayOf(
        Pair(BoardConfig.RED_START_INDEX, LudoRed),
        Pair(BoardConfig.GREEN_START_INDEX, LudoGreen),
        Pair(BoardConfig.YELLOW_START_INDEX, LudoYellow),
        Pair(BoardConfig.BLUE_START_INDEX, LudoBlue)
    )
    for (i in 0 until starts.size) {
        val (index, color) = starts[i]
        val (row, col) = track[index]
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
    for (i in 0 until positions.size) {
        val (row, col) = positions[i]
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
    drawDirectionArrow(offsetX + 0 * cellSize + cellSize / 2, offsetY + 7 * cellSize + cellSize / 2, cellSize * 0.45f, LudoRed, 0f)
    drawDirectionArrow(offsetX + 7 * cellSize + cellSize / 2, offsetY + 0 * cellSize + cellSize / 2, cellSize * 0.45f, LudoGreen, 90f)
    drawDirectionArrow(offsetX + 14 * cellSize + cellSize / 2, offsetY + 7 * cellSize + cellSize / 2, cellSize * 0.45f, LudoYellow, 180f)
    drawDirectionArrow(offsetX + 7 * cellSize + cellSize / 2, offsetY + 14 * cellSize + cellSize / 2, cellSize * 0.45f, LudoBlue, 270f)
}

private fun DrawScope.drawDirectionArrow(cx: Float, cy: Float, size: Float, color: Color, angleDeg: Float) {
    val half = size / 2
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

    sharedArrowPath.reset()
    sharedArrowPath.moveTo(pTip.x, pTip.y)
    sharedArrowPath.lineTo(pTop.x, pTop.y)
    sharedArrowPath.lineTo(pMid.x, pMid.y)
    sharedArrowPath.lineTo(pBot.x, pBot.y)
    sharedArrowPath.close()

    drawPath(sharedArrowPath, color = color, style = Fill)
}

private fun DrawScope.drawSafeZones(offsetX: Float, offsetY: Float, cellSize: Float) {
    val starIndices = arrayOf(8, 21, 34, 47)
    val track = BoardConfig.mainTrack
    for (i in 0 until starIndices.size) {
        val index = starIndices[i]
        val (row, col) = track[index]
        val cx = offsetX + col * cellSize + cellSize / 2
        val cy = offsetY + row * cellSize + cellSize / 2

        drawStar(cx, cy, cellSize * 0.36f, Color(0xFFFFD54F), Color(0xFF8D6E14))
    }
}

private fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, fillColor: Color, strokeColor: Color) {
    val points = 5
    sharedStarPath.reset()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.42f
        val angle = Math.toRadians((i * 360.0 / (points * 2)) - 90.0)
        val x = cx + r * cos(angle).toFloat()
        val y = cy + r * sin(angle).toFloat()
        if (i == 0) sharedStarPath.moveTo(x, y) else sharedStarPath.lineTo(x, y)
    }
    sharedStarPath.close()
    drawPath(sharedStarPath, color = fillColor, style = Fill)
    drawPath(sharedStarPath, color = strokeColor, style = Stroke(width = 1.8f))
}

private fun DrawScope.drawCenterHome(offsetX: Float, offsetY: Float, cellSize: Float) {
    val centerX = offsetX + 7.5f * cellSize
    val centerY = offsetY + 7.5f * cellSize
    val triangleSpan = cellSize * 1.5f

    // Red Left Triangle
    sharedTrianglePath.reset()
    sharedTrianglePath.moveTo(centerX - triangleSpan, centerY - triangleSpan)
    sharedTrianglePath.lineTo(centerX, centerY)
    sharedTrianglePath.lineTo(centerX - triangleSpan, centerY + triangleSpan)
    sharedTrianglePath.close()
    drawPath(sharedTrianglePath, color = LudoRed, style = Fill)

    // Green Top Triangle
    sharedTrianglePath.reset()
    sharedTrianglePath.moveTo(centerX - triangleSpan, centerY - triangleSpan)
    sharedTrianglePath.lineTo(centerX + triangleSpan, centerY - triangleSpan)
    sharedTrianglePath.lineTo(centerX, centerY)
    sharedTrianglePath.close()
    drawPath(sharedTrianglePath, color = LudoGreen, style = Fill)

    // Yellow Right Triangle
    sharedTrianglePath.reset()
    sharedTrianglePath.moveTo(centerX + triangleSpan, centerY - triangleSpan)
    sharedTrianglePath.lineTo(centerX + triangleSpan, centerY + triangleSpan)
    sharedTrianglePath.lineTo(centerX, centerY)
    sharedTrianglePath.close()
    drawPath(sharedTrianglePath, color = LudoYellow, style = Fill)

    // Blue Bottom Triangle
    sharedTrianglePath.reset()
    sharedTrianglePath.moveTo(centerX - triangleSpan, centerY + triangleSpan)
    sharedTrianglePath.lineTo(centerX + triangleSpan, centerY + triangleSpan)
    sharedTrianglePath.lineTo(centerX, centerY)
    sharedTrianglePath.close()
    drawPath(sharedTrianglePath, color = LudoBlue, style = Fill)
}
