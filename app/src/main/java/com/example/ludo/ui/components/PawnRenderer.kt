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

private val PinSilverTop = Color(0xFFFFFFFF)
private val PinSilverMid = Color(0xFFE9EDF2)
private val PinSilverBottom = Color(0xFFB9C2CC)
private val PinOutline = Color(0xFF5B6470)

/** Vertical distance from a goti's anchor to its visual middle; add it to centre a goti in a box. */
internal fun pawnVisualCenterOffset(radius: Float) = radius * 0.62f

/**
 * Classic Ludo-King-style goti: a glossy silver map pin holding a coloured gem, standing with
 * its tip on a ring of the player's colour.
 *
 * @param center anchor point: the ground ring sits here and the pin rises above it.
 * @param radius overall scale; the pin is about 1.25 × radius wide and 1.9 × radius tall.
 * @param lift how far (px) the pin is raised off the board, e.g. mid-hop; ring and shadow stay put.
 * @param selectable 0..1 strength of the "you can move me" halo around the ground ring.
 * @param squash 0..1 landing squash: the pin briefly flattens as it touches down.
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
    val groundY = center.y + r * 0.3f
    val liftAmount = (lift / (r * 2.2f)).coerceIn(0f, 1f)

    // 1. Ground: soft shadow plus the coloured ring the pin stands on.
    val ringW = r * 1.3f
    val ringH = r * 0.5f
    drawOval(
        color = Color.Black.copy(alpha = 0.25f * (1f - 0.6f * liftAmount)),
        topLeft = Offset(cx - ringW * 0.55f, groundY - ringH * 0.35f),
        size = Size(ringW * 1.1f, ringH * 0.95f)
    )
    if (selectable > 0f) {
        val haloW = ringW * (1.25f + 0.3f * selectable)
        val haloH = ringH * (1.25f + 0.3f * selectable)
        drawOval(
            color = c.baseColor.copy(alpha = 0.25f + 0.35f * (1f - selectable)),
            topLeft = Offset(cx - haloW / 2, groundY - haloH / 2),
            size = Size(haloW, haloH),
            style = Stroke(r * 0.12f)
        )
    }
    drawOval(c.darkColor, Offset(cx - ringW / 2, groundY - ringH / 2), Size(ringW, ringH), style = Stroke(r * 0.16f))
    drawOval(c.lightColor.copy(alpha = 0.7f), Offset(cx - ringW / 2, groundY - ringH / 2), Size(ringW, ringH * 0.9f), style = Stroke(r * 0.05f))

    // 2. The pin: squash flattens and widens it briefly on landing.
    val stretchY = 1f - 0.14f * squash
    val stretchX = 1f + 0.1f * squash
    val tipY = groundY - lift
    val headR = r * 0.62f * stretchX
    val headY = tipY - r * 1.25f * stretchY
    buildPin(cx, tipY, headY, headR)

    drawPath(
        pinPath,
        Brush.linearGradient(
            listOf(PinSilverTop, PinSilverMid, PinSilverBottom),
            start = Offset(cx - headR, headY - headR),
            end = Offset(cx + headR, tipY)
        )
    )
    drawPath(pinPath, PinOutline, style = Stroke(width = r * 0.06f))

    // 3. Coloured gem set in the head.
    val gemR = headR * 0.68f
    val gem = Offset(cx, headY)
    drawCircle(c.deepShadow.copy(alpha = 0.55f), gemR * 1.08f, Offset(gem.x, gem.y + gemR * 0.08f))
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

    // 4. Gloss along the pin's upper-left rim.
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

/** Teardrop pin: round head centred at [headY] tapering to a point at [tipY]. */
private fun buildPin(cx: Float, tipY: Float, headY: Float, headR: Float) {
    pinPath.reset()
    pinPath.moveTo(cx, tipY)
    pinPath.cubicTo(cx - headR * 0.3f, tipY - headR * 0.35f, cx - headR, headY + headR * 0.75f, cx - headR, headY)
    pinPath.arcTo(Rect(cx - headR, headY - headR, cx + headR, headY + headR), 180f, 180f, false)
    pinPath.cubicTo(cx + headR, headY + headR * 0.75f, cx + headR * 0.3f, tipY - headR * 0.35f, cx, tipY)
    pinPath.close()
}
