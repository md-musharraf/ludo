package com.example.ludo.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ludo.core.util.PawnColorScheme
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.model.PlayerColor

// Reused across draws (main thread only) to avoid per-frame Path allocations.
private val sharedStemPath = Path()

/**
 * Renders an authentic, realistic 3D Classic Ludo Pawn (Goti).
 * True Halma/Chess Pawn structure with physical grounding:
 * 1. Deep Ambient Occlusion Contact Shadow
 * 2. Stepped Pedestal Base Disc
 * 3. Tapered Waist Stem with Directional 5-stop Lighting
 * 4. Toroidal Collar Ring
 * 5. Spherical Crown Head with 3D Radial Depth & Specular Shine
 */
internal fun DrawScope.draw3DClassicPawn(
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
