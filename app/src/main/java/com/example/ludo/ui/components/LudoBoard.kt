package com.example.ludo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.engine.BoardConfig
import com.example.ludo.model.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Classic 3D Ludo board, rendered as two layers:
 * 1. [BoardBackdrop]: the static board in its own graphics layer. It reads no state, so it is
 *    recorded once per size and replayed from the RenderNode afterwards.
 * 2. Pawn layer: the turn halo, pawns and hop animation. Hop progress and pulse are read only in
 *    the draw phase, so animating redraws this layer without recomposition.
 */
@Composable
fun LudoBoard(
    gameState: GameState,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayer = gameState.currentPlayer
    val humanChoosing = gameState.gamePhase == GamePhase.WAITING_FOR_MOVE && currentPlayer?.isAI == false
    val pulse by rememberPulse(active = humanChoosing, from = 0f, to = 1f, durationMs = 600, idle = 0.6f)

    val spots = remember(gameState) { layoutPawns(gameState) }

    val hop = gameState.hop
    // Keyed on seq so a new hop starts at 0 in the same frame it appears (no one-frame jump).
    val hopProgress = remember(hop?.seq) { Animatable(0f) }
    LaunchedEffect(hop?.seq) {
        if (hop != null) hopProgress.animateTo(1f, tween(hop.durationMs, easing = LinearEasing))
    }

    val latestSpots by rememberUpdatedState(spots)
    val latestOnTokenClick by rememberUpdatedState(onTokenClick)

    Box(modifier) {
        BoardBackdrop()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val g = BoardGeometry(Size(size.width.toFloat(), size.height.toFloat()))
                        val col = (tap.x - g.offsetX) / g.cell
                        val row = (tap.y - g.offsetY) / g.cell
                        latestSpots
                            .filter { it.isValid }
                            .map { it to hypot(it.cx - col, it.cy - row) }
                            .filter { (_, d) -> d <= TAP_RADIUS_CELLS }
                            .minByOrNull { (_, d) -> d }
                            ?.let { (spot, _) -> latestOnTokenClick(spot.tokenId) }
                    }
                }
        ) {
            val g = BoardGeometry(size)
            val pulseAlpha = 0.35f + 0.6f * pulse
            val pulseScale = 1f + 0.18f * pulse

            currentPlayer?.let { drawTurnHalo(g, it.color, pulseAlpha) }

            for (spot in spots) {
                val center = g.point(spot.cx, spot.cy)
                draw3DClassicPawn(
                    center = center,
                    radius = spot.radius * g.cell,
                    playerColor = spot.color,
                    isValid = spot.isValid,
                    pulseAlpha = pulseAlpha,
                    pulseScale = pulseScale,
                    groundCenter = center,
                    hopHeight = 0f,
                    isAnimating = false
                )
            }

            if (hop != null) {
                val color = gameState.players.firstOrNull { it.id == hop.playerId }?.color ?: return@Canvas
                drawHoppingPawn(g, hop, color, hopProgress.value)
            }
        }
    }
}

/** Static board art. Parameterless so it never recomposes; its own layer caches the drawing. */
@Composable
private fun BoardBackdrop() {
    Canvas(Modifier.fillMaxSize().graphicsLayer()) {
        val g = BoardGeometry(size)
        drawBoardBase(g)
        for (color in PlayerColor.entries) drawHomeBase(g, color)
        drawTrackCells(g)
        drawStartingSquares(g)
        drawSafeZones(g)
        for (color in PlayerColor.entries) {
            drawHomeColumn(g, BoardConfig.homeColumns[color.ordinal], PlayerColorUtils.getComposeColor(color))
        }
        drawTrackArrows(g)
        drawCenterHome(g)
        drawGoldenBoardFrame(g)
    }
}

// region Layout

private const val TAP_RADIUS_CELLS = 1.1f

/** Square board fitted and centred in the canvas; board coordinates are in cell units. */
private class BoardGeometry(size: Size) {
    val boardSize = min(size.width, size.height)
    val cell = boardSize / BoardConfig.BOARD_SIZE
    val offsetX = (size.width - boardSize) / 2f
    val offsetY = (size.height - boardSize) / 2f

    fun x(col: Float) = offsetX + col * cell
    fun y(row: Float) = offsetY + row * cell
    fun point(col: Float, row: Float) = Offset(x(col), y(row))
}

/** A pawn resting on the board. [cx]/[cy]/[radius] are in cell units (cell centre = index + 0.5). */
private class PawnSpot(
    val playerId: Int,
    val tokenId: Int,
    val color: PlayerColor,
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val isValid: Boolean
)

// Finished pawns line up inside their colour's centre triangle: (col, row, lineIsHorizontal).
private val GoalAnchors = arrayOf(
    Triple(6.45f, 7.5f, false), // Red, left triangle
    Triple(7.5f, 6.45f, true),  // Green, top triangle
    Triple(8.55f, 7.5f, false), // Yellow, right triangle
    Triple(7.5f, 8.55f, true)   // Blue, bottom triangle
)

private fun layoutPawns(state: GameState): List<PawnSpot> {
    val current = state.currentPlayer
    val humanChoosing = state.gamePhase == GamePhase.WAITING_FOR_MOVE && current?.isAI == false
    val hop = state.hop
    val spots = ArrayList<PawnSpot>(16)
    val byCell = LinkedHashMap<Pair<Int, Int>, MutableList<Pair<Player, Token>>>()

    for (player in state.players) {
        val finished = player.tokens.filter { it.state == TokenState.FINISHED }
        for (token in player.tokens) {
            if (hop != null && hop.playerId == player.id && hop.tokenId == token.id) continue
            val isValid = humanChoosing && player.id == current?.id && token.id in state.validMoves
            when (token.state) {
                TokenState.IN_HOME -> {
                    val (row, col) = BoardConfig.homeSpot(player.color.ordinal, token.id)
                    spots += PawnSpot(player.id, token.id, player.color, col + 0.5f, row + 0.5f, 0.44f, isValid)
                }
                TokenState.ON_BOARD, TokenState.IN_HOME_COLUMN -> {
                    token.boardPosition?.let { byCell.getOrPut(it) { ArrayList(2) }.add(player to token) }
                }
                TokenState.FINISHED -> {
                    val (ax, ay, horizontal) = GoalAnchors[player.color.ordinal]
                    val shift = (finished.indexOf(token) - (finished.size - 1) / 2f) * 0.34f
                    val cx = if (horizontal) ax + shift else ax
                    val cy = if (horizontal) ay else ay + shift
                    spots += PawnSpot(player.id, token.id, player.color, cx, cy, 0.2f, false)
                }
            }
        }
    }

    for ((cell, occupants) in byCell) {
        val (row, col) = cell
        occupants.forEachIndexed { index, (player, token) ->
            val (dx, dy, radius) = stackOffset(occupants.size, index)
            val isValid = humanChoosing && player.id == current?.id && token.id in state.validMoves
            spots += PawnSpot(player.id, token.id, player.color, col + 0.5f + dx, row + 0.5f + dy, radius, isValid)
        }
    }

    // Paint back-to-front so lower pawns overlap the ones above them.
    spots.sortBy { it.cy }
    return spots
}

/** Offset (cell units) and radius for the [index]-th of [count] pawns sharing a cell. */
private fun stackOffset(count: Int, index: Int): Triple<Float, Float, Float> = when (count) {
    1 -> Triple(0f, 0f, 0.44f)
    2 -> if (index == 0) Triple(-0.16f, -0.16f, 0.32f) else Triple(0.16f, 0.16f, 0.32f)
    3 -> when (index) {
        0 -> Triple(0f, -0.16f, 0.28f)
        1 -> Triple(-0.16f, 0.144f, 0.28f)
        else -> Triple(0.16f, 0.144f, 0.28f)
    }
    else -> when (index % 4) {
        0 -> Triple(-0.18f, -0.18f, 0.24f)
        1 -> Triple(0.18f, -0.18f, 0.24f)
        2 -> Triple(-0.18f, 0.18f, 0.24f)
        else -> Triple(0.18f, 0.18f, 0.24f)
    }
}

// endregion

// region Dynamic layer

private fun DrawScope.drawHoppingPawn(g: BoardGeometry, hop: HopMove, color: PlayerColor, progress: Float) {
    val eased = (1f - cos(progress * PI.toFloat())) / 2f
    val fromCol = hop.from.second + 0.5f
    val fromRow = hop.from.first + 0.5f
    val toCol = hop.to.second + 0.5f
    val toRow = hop.to.first + 0.5f

    val ground = g.point(fromCol + (toCol - fromCol) * eased, fromRow + (toRow - fromRow) * eased)
    // Longer journeys (e.g. a captured pawn flying home) arc higher.
    val distance = hypot(toCol - fromCol, toRow - fromRow)
    val arc = sin(progress * PI.toFloat())
    val hopHeight = arc * g.cell * (0.8f + 0.12f * distance).coerceAtMost(2.2f)

    draw3DClassicPawn(
        center = Offset(ground.x, ground.y - hopHeight),
        radius = g.cell * 0.44f * (1f + arc * 0.2f),
        playerColor = color,
        isValid = false,
        pulseAlpha = 0f,
        pulseScale = 1f,
        groundCenter = ground,
        hopHeight = hopHeight,
        isAnimating = true
    )
}

/** Top-left cell (row, col) of each colour's 6x6 base, by colour ordinal. */
private val BaseOrigins = arrayOf(0 to 0, 0 to 9, 9 to 9, 9 to 0)

private fun DrawScope.drawTurnHalo(g: BoardGeometry, color: PlayerColor, pulseAlpha: Float) {
    val (row, col) = BaseOrigins[color.ordinal]
    val homeSize = g.cell * 6
    drawRoundRect(
        color = PlayerColorUtils.getComposeColor(color).copy(alpha = pulseAlpha * 0.65f),
        topLeft = Offset(g.x(col.toFloat()) - 3f, g.y(row.toFloat()) - 3f),
        size = Size(homeSize + 6f, homeSize + 6f),
        cornerRadius = CornerRadius(g.cell * 0.45f),
        style = Stroke(width = 3.5f)
    )
}

// endregion

// region Static board art

// Main-thread-only scratch paths for the static layer.
private val sharedArrowPath = Path()
private val sharedStarPath = Path()
private val sharedTrianglePath = Path()

private fun DrawScope.drawBoardBase(g: BoardGeometry) {
    drawRoundRect(
        color = Color(0x35000000),
        topLeft = Offset(g.offsetX + 3f, g.offsetY + 6f),
        size = Size(g.boardSize, g.boardSize),
        cornerRadius = CornerRadius(g.cell * 0.50f)
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(g.offsetX, g.offsetY),
        size = Size(g.boardSize, g.boardSize),
        cornerRadius = CornerRadius(g.cell * 0.50f)
    )
}

private fun DrawScope.drawHomeBase(g: BoardGeometry, player: PlayerColor) {
    val color = PlayerColorUtils.getComposeColor(player)
    val (row, col) = BaseOrigins[player.ordinal]
    val cellSize = g.cell
    val x = g.x(col.toFloat())
    val y = g.y(row.toFloat())
    val homeSize = cellSize * 6

    // Outer Colored Rounded Frame
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(homeSize, homeSize),
        cornerRadius = CornerRadius(cellSize * 0.45f)
    )

    // Inner Tinted Platform
    val innerMargin = cellSize * 0.85f
    val innerSize = homeSize - innerMargin * 2
    drawRoundRect(
        color = PlayerColorUtils.getLightColor(player),
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
    for ((spotRow, spotCol) in BoardConfig.homePositions.getValue(player.ordinal)) {
        val center = g.point(spotCol + 0.5f, spotRow + 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x35000000), Color(0x60000000)),
                center = Offset(center.x - spotRadius * 0.2f, center.y - spotRadius * 0.2f),
                radius = spotRadius
            ),
            radius = spotRadius,
            center = center
        )
        drawCircle(color = color.copy(alpha = 0.85f), radius = spotRadius, center = center, style = Stroke(width = 3.2f))
        drawCircle(color = color.copy(alpha = 0.18f), radius = spotRadius * 0.82f, center = center)
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

private fun DrawScope.drawGoldenBoardFrame(g: BoardGeometry) {
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF8D6E14), Color(0xFFF9D976), Color(0xFFD4AF37), Color(0xFF8D6E14)),
            start = Offset(g.offsetX, g.offsetY),
            end = Offset(g.offsetX + g.boardSize, g.offsetY + g.boardSize)
        ),
        topLeft = Offset(g.offsetX - 2f, g.offsetY - 2f),
        size = Size(g.boardSize + 4f, g.boardSize + 4f),
        cornerRadius = CornerRadius(g.cell * 0.50f),
        style = Stroke(width = 4.0f)
    )
    drawRoundRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(g.offsetX, g.offsetY),
        size = Size(g.boardSize, g.boardSize),
        cornerRadius = CornerRadius(g.cell * 0.50f),
        style = Stroke(width = 1.5f)
    )
}

/** Fills a cell and outlines it; shared by track, start squares and home columns. */
private fun DrawScope.drawCell(g: BoardGeometry, cell: Pair<Int, Int>, fill: Color, stroke: Color, strokeWidth: Float) {
    val topLeft = g.point(cell.second.toFloat(), cell.first.toFloat())
    val size = Size(g.cell, g.cell)
    drawRect(color = fill, topLeft = topLeft, size = size)
    drawRect(color = stroke, topLeft = topLeft, size = size, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawTrackCells(g: BoardGeometry) {
    for (cell in BoardConfig.mainTrack) drawCell(g, cell, Color.White, Color(0xFFCFD8DC), 1f)
}

private fun DrawScope.drawStartingSquares(g: BoardGeometry) {
    for (color in PlayerColor.entries) {
        val cell = BoardConfig.mainTrack[BoardConfig.startIndices[color.ordinal]]
        drawCell(g, cell, PlayerColorUtils.getComposeColor(color), Color(0xFFB0BEC5), 1.2f)
    }
}

private fun DrawScope.drawHomeColumn(g: BoardGeometry, positions: List<Pair<Int, Int>>, color: Color) {
    for (cell in positions) drawCell(g, cell, color, Color.White.copy(alpha = 0.5f), 1f)
}

// Entry arrows pointing into each home column: (col, row, angle) by colour ordinal.
private val ArrowSpecs = arrayOf(Triple(0, 7, 0f), Triple(7, 0, 90f), Triple(14, 7, 180f), Triple(7, 14, 270f))

private fun DrawScope.drawTrackArrows(g: BoardGeometry) {
    for (color in PlayerColor.entries) {
        val (col, row, angle) = ArrowSpecs[color.ordinal]
        drawDirectionArrow(g.point(col + 0.5f, row + 0.5f), g.cell * 0.45f, PlayerColorUtils.getComposeColor(color), angle)
    }
}

private fun DrawScope.drawDirectionArrow(center: Offset, size: Float, color: Color, angleDeg: Float) {
    val half = size / 2
    val rad = Math.toRadians(angleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    fun rotated(px: Float, py: Float) = Offset(center.x + px * cosA - py * sinA, center.y + px * sinA + py * cosA)

    val tip = rotated(half, 0f)
    val top = rotated(-half, -half * 0.7f)
    val mid = rotated(-half * 0.4f, 0f)
    val bottom = rotated(-half, half * 0.7f)

    sharedArrowPath.reset()
    sharedArrowPath.moveTo(tip.x, tip.y)
    sharedArrowPath.lineTo(top.x, top.y)
    sharedArrowPath.lineTo(mid.x, mid.y)
    sharedArrowPath.lineTo(bottom.x, bottom.y)
    sharedArrowPath.close()
    drawPath(sharedArrowPath, color = color, style = Fill)
}

private fun DrawScope.drawSafeZones(g: BoardGeometry) {
    for (index in BoardConfig.starSpotIndices) {
        val (row, col) = BoardConfig.mainTrack[index]
        drawStar(g.point(col + 0.5f, row + 0.5f), g.cell * 0.36f, Color(0xFFFFD54F), Color(0xFF8D6E14))
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, fillColor: Color, strokeColor: Color) {
    val points = 5
    sharedStarPath.reset()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.42f
        val angle = Math.toRadians((i * 360.0 / (points * 2)) - 90.0)
        val x = center.x + r * cos(angle).toFloat()
        val y = center.y + r * sin(angle).toFloat()
        if (i == 0) sharedStarPath.moveTo(x, y) else sharedStarPath.lineTo(x, y)
    }
    sharedStarPath.close()
    drawPath(sharedStarPath, color = fillColor, style = Fill)
    drawPath(sharedStarPath, color = strokeColor, style = Stroke(width = 1.8f))
}

// Outer corners (col, row offsets from centre, in cells) of each colour's centre triangle.
private val TriangleCorners = arrayOf(
    floatArrayOf(-1.5f, -1.5f, -1.5f, 1.5f), // Red, left
    floatArrayOf(-1.5f, -1.5f, 1.5f, -1.5f), // Green, top
    floatArrayOf(1.5f, -1.5f, 1.5f, 1.5f),   // Yellow, right
    floatArrayOf(-1.5f, 1.5f, 1.5f, 1.5f)    // Blue, bottom
)

private fun DrawScope.drawCenterHome(g: BoardGeometry) {
    val center = g.point(7.5f, 7.5f)
    for (color in PlayerColor.entries) {
        val c = TriangleCorners[color.ordinal]
        sharedTrianglePath.reset()
        sharedTrianglePath.moveTo(center.x + c[0] * g.cell, center.y + c[1] * g.cell)
        sharedTrianglePath.lineTo(center.x + c[2] * g.cell, center.y + c[3] * g.cell)
        sharedTrianglePath.lineTo(center.x, center.y)
        sharedTrianglePath.close()
        drawPath(sharedTrianglePath, color = PlayerColorUtils.getComposeColor(color), style = Fill)
    }
}

// endregion
