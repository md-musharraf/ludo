package com.example.ludo.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.model.PlayerColor

// Reused across draws (main thread only) to avoid per-frame Path allocations.
private val pinPath = Path()

private val PinLight = Color(0xFFFFFFFF)
private val PinMid = Color(0xFFE6EAEF)
private val PinShade = Color(0xFFAEB7C2)
private val PinOutline = Color(0xFF56606C)

// Pin geometry, in multiples of the radius r (y grows downward from the anchor).
internal const val PAWN_GROUND = 0.3f   // Base top face centre, below the anchor
private const val BASE_W = 1.24f        // Base disc width
private const val BASE_H = 0.42f        // Base disc top-face height (perspective ellipse)
private const val BASE_THICK = 0.14f    // Visible side of the base disc
private const val PIN_HEIGHT = 1.36f    // Tip to head centre
private const val HEAD_R = 0.56f        // Head radius

/** Extent above the anchor (head top) and below it (base underside), in multiples of r. */
internal const val PAWN_TOP = PIN_HEIGHT + HEAD_R - PAWN_GROUND + 0.04f
internal const val PAWN_BOTTOM = PAWN_GROUND + BASE_H / 2 + BASE_THICK + 0.04f

/** Vertical distance from a goti's anchor to its visual middle; add it to centre a goti in a box. */
internal fun pawnVisualCenterOffset(radius: Float) = radius * (PAWN_TOP - PAWN_BOTTOM) / 2

/**
 * Classic Ludo-King-style goti standing upright: a glossy silver pin holding a coloured gem, its
 * tip set into a solid 3D base disc of the player's colour.
 *
 * @param center anchor point; the base disc sits just below it and the pin rises above.
 * @param radius overall scale; the goti is about 1.24 × radius wide and 2.3 × radius tall.
 * @param lift how far (px) the goti is raised, e.g. mid-hop; only the shadow stays on the board.
 * @param selectable 0..1 strength of the "you can move me" halo around the base.
 * @param squash 0..1 landing squash: the goti briefly flattens as it touches down.
 */
internal fun DrawScope.drawPawn(
    center: Offset,
    radius: Float,
    color: PlayerColor,
    lift: Float = 0f,
    selectable: Float = 0f,
    squash: Float = 0f
) {
    val c = PlayerColorUtils.getPawnColorScheme(color)
    val r = radius
    val cx = center.x
    val floorY = center.y + r * PAWN_GROUND                 // Where the base rests on the board
    val liftAmount = (lift / (r * 2.2f)).coerceIn(0f, 1f)
    val stretchY = 1f - 0.14f * squash
    val stretchX = 1f + 0.08f * squash

    val baseW = r * BASE_W * stretchX
    val baseH = r * BASE_H
    val thick = r * BASE_THICK * stretchY

    // 1. Soft shadow on the board; shrinks and fades as the goti rises.
    val shadowW = baseW * 1.15f * (1f - 0.3f * liftAmount)
    val shadowH = baseH * 1.1f * (1f - 0.3f * liftAmount)
    drawOval(
        color = Color.Black.copy(alpha = 0.28f * (1f - 0.6f * liftAmount)),
        topLeft = Offset(cx - shadowW / 2, floorY + thick - shadowH * 0.35f),
        size = Size(shadowW, shadowH)
    )

    // 2. Selection halo around the base.
    if (selectable > 0f) {
        val haloW = baseW * (1.3f + 0.3f * selectable)
        val haloH = baseH * (1.3f + 0.3f * selectable)
        drawOval(
            color = c.baseColor.copy(alpha = 0.3f + 0.4f * (1f - selectable)),
            topLeft = Offset(cx - haloW / 2, floorY + thick / 2 - haloH / 2),
            size = Size(haloW, haloH),
            style = Stroke(r * 0.12f)
        )
    }

    val baseY = floorY - lift                          // Base top face centre (moves with lift)

    // 3. Base disc: dark side, then a lit top face with a bright rim and a socket for the tip.
    drawOval(c.deepShadow, Offset(cx - baseW / 2, baseY - baseH / 2 + thick), Size(baseW, baseH))
    drawRect(c.deepShadow, Offset(cx - baseW / 2, baseY), Size(baseW, thick))
    drawOval(
        brush = Brush.horizontalGradient(listOf(c.lightColor, c.baseColor, c.darkColor), startX = cx - baseW / 2, endX = cx + baseW / 2),
        topLeft = Offset(cx - baseW / 2, baseY - baseH / 2),
        size = Size(baseW, baseH)
    )
    drawOval(c.highlightColor.copy(alpha = 0.7f), Offset(cx - baseW / 2, baseY - baseH / 2), Size(baseW, baseH), style = Stroke(r * 0.05f))
    drawOval(c.deepShadow.copy(alpha = 0.6f), Offset(cx - baseW * 0.2f, baseY - baseH * 0.2f), Size(baseW * 0.4f, baseH * 0.4f))

    // 4. Upright pin: tip in the base socket, head above. Cylindrical shading, lit from the left.
    val headR = r * HEAD_R * stretchX
    val tipY = baseY
    val headY = tipY - r * PIN_HEIGHT * stretchY
    buildPin(cx, tipY, headY, headR)
    drawPath(
        pinPath,
        Brush.horizontalGradient(
            listOf(PinMid, PinLight, PinMid, PinShade),
            startX = cx - headR,
            endX = cx + headR
        )
    )
    drawPath(pinPath, PinOutline, style = Stroke(width = r * 0.06f))

    // 5. Coloured gem set in the head.
    val gemR = headR * 0.66f
    val gem = Offset(cx, headY)
    drawCircle(c.deepShadow.copy(alpha = 0.55f), gemR * 1.1f, Offset(gem.x, gem.y + gemR * 0.08f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(c.highlightColor, c.lightColor, c.baseColor, c.darkColor),
            center = Offset(gem.x - gemR * 0.35f, gem.y - gemR * 0.4f),
            radius = gemR * 1.6f
        ),
        radius = gemR,
        center = gem
    )
    drawOval(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(gem.x - gemR * 0.55f, gem.y - gemR * 0.68f),
        size = Size(gemR * 0.55f, gemR * 0.36f)
    )

    // 6. Gloss along the head's upper-left rim.
    drawArc(
        color = Color.White.copy(alpha = 0.9f),
        startAngle = 190f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(cx - headR * 0.86f, headY - headR * 0.86f),
        size = Size(headR * 1.72f, headR * 1.72f),
        style = Stroke(width = r * 0.07f)
    )
}

/** Upright teardrop pin: round head centred at [headY] tapering to a point at [tipY]. */
private fun buildPin(cx: Float, tipY: Float, headY: Float, headR: Float) {
    pinPath.reset()
    pinPath.moveTo(cx, tipY)
    pinPath.cubicTo(cx - headR * 0.22f, tipY - headR * 0.45f, cx - headR, headY + headR * 0.85f, cx - headR, headY)
    pinPath.arcTo(Rect(cx - headR, headY - headR, cx + headR, headY + headR), 180f, 180f, false)
    pinPath.cubicTo(cx + headR, headY + headR * 0.85f, cx + headR * 0.22f, tipY - headR * 0.45f, cx, tipY)
    pinPath.close()
}
