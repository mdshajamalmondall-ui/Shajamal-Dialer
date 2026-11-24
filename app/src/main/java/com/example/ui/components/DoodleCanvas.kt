package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.data.model.DoodleDesign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun DoodleCanvas(
    design: DoodleDesign,
    tintColor: Color,
    isLightTheme: Boolean,
    modifier: Modifier = Modifier
) {
    if (design == DoodleDesign.NONE) return

    val strokeColor = tintColor.copy(alpha = if (isLightTheme) 0.14f else 0.16f)
    val accentStroke = tintColor.copy(alpha = if (isLightTheme) 0.09f else 0.11f)

    key(design, strokeColor, accentStroke) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawWithCache {
                    val cell = (minOf(size.width, size.height) / 3.4f).coerceAtLeast(160f)
                    val tilePx = (cell * 2f).roundToInt().coerceAtLeast(320)
                    val tileFloat = tilePx.toFloat()
                    val halfCell = tileFloat * 0.25f
                    val step = tileFloat * 0.5f

                    val tileBitmap = ImageBitmap(tilePx, tilePx, ImageBitmapConfig.Argb8888)
                    val canvas = Canvas(tileBitmap)
                    val canvasDrawScope = CanvasDrawScope()

                    val placements = listOf(
                        // Row 0: safely centered inside tile bounds
                        Triple(halfCell, halfCell, 0),
                        Triple(halfCell + step, halfCell, 1),
                        // Row 1: staggered by half cell; index 3 wraps seamlessly across x = 0 and x = tileFloat
                        Triple(step, halfCell + step, 2),
                        Triple(0f, halfCell + step, 3),
                        Triple(tileFloat, halfCell + step, 3)
                    )

                    canvasDrawScope.draw(
                        density = this,
                        layoutDirection = layoutDirection,
                        canvas = canvas,
                        size = Size(tileFloat, tileFloat)
                    ) {
                        for ((cx, cy, idx) in placements) {
                            val r = if (idx < 2) 0 else 1
                            val c = if (idx == 3) 0 else idx % 2
                            val angle = ((r * 37 + c * 53) % 45 - 22).toFloat()
                            val color = if (idx % 2 == 0) strokeColor else accentStroke

                            when (design) {
                                DoodleDesign.NONE -> Unit
                                DoodleDesign.PHONE_DOODLE -> drawPhoneDoodle(cx, cy, step * 0.24f, angle, color)
                                DoodleDesign.COSMIC_STAR -> drawCosmicStar(cx, cy, step * 0.25f, idx % 3, color)
                                DoodleDesign.FLORAL_LEAVES -> drawFloralLeaf(cx, cy, step * 0.26f, angle, color)
                                DoodleDesign.GEOMETRIC_HEARTS -> drawGeometricHeart(cx, cy, step * 0.22f, angle, color)
                                DoodleDesign.ROYAL_MANDALA -> drawRoyalMandala(cx, cy, step * 0.28f, color)
                                DoodleDesign.OCEAN_RIPPLES -> drawOceanRipples(cx, cy, step * 0.28f, color)
                                DoodleDesign.CYBER_CIRCUIT -> drawCyberCircuit(cx, cy, step * 0.3f, idx, color)
                                DoodleDesign.MUSIC_BEAT -> drawMusicBeat(cx, cy, step * 0.23f, angle, color)
                                DoodleDesign.BUTTERFLY -> drawButterfly(cx, cy, step * 0.25f, angle, color)
                            }
                        }
                    }

                    val shaderBrush = ShaderBrush(
                        ImageShader(
                            image = tileBitmap,
                            tileModeX = TileMode.Repeated,
                            tileModeY = TileMode.Repeated
                        )
                    )

                    onDrawBehind {
                        drawRect(brush = shaderBrush)
                    }
                }
        )
    }
}

private fun DrawScope.drawPhoneDoodle(cx: Float, cy: Float, r: Float, angle: Float, color: Color) {
    rotate(angle, pivot = Offset(cx, cy)) {
        val stroke = Stroke(width = 2.4f, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(cx - r * 0.6f, cy - r * 0.65f)
            cubicTo(cx - r * 0.9f, cy - r * 0.2f, cx - r * 0.2f, cy + r * 0.9f, cx + r * 0.65f, cy + r * 0.6f)
            lineTo(cx + r * 0.78f, cy + r * 0.28f)
            lineTo(cx + r * 0.4f, cy + r * 0.14f)
            lineTo(cx + r * 0.2f, cy + r * 0.28f)
            cubicTo(cx - r * 0.05f, cy + r * 0.15f, cx - r * 0.2f, cy - r * 0.05f, cx - r * 0.28f, cy - r * 0.25f)
            lineTo(cx - r * 0.12f, cy - r * 0.45f)
            lineTo(cx - r * 0.28f, cy - r * 0.78f)
            close()
        }
        drawPath(path, color = color, style = stroke)
        drawArc(
            color = color,
            startAngle = -75f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(cx - r * 0.1f, cy - r * 0.95f),
            size = Size(r * 1.1f, r * 1.1f),
            style = stroke
        )
    }
}

private fun DrawScope.drawCosmicStar(cx: Float, cy: Float, r: Float, index: Int, color: Color) {
    val stroke = Stroke(width = 2.2f, cap = StrokeCap.Round)
    when (index % 3) {
        0 -> {
            // 4-point sparkle star
            val path = Path().apply {
                moveTo(cx, cy - r)
                quadraticTo(cx, cy, cx + r, cy)
                quadraticTo(cx, cy, cx, cy + r)
                quadraticTo(cx, cy, cx - r, cy)
                quadraticTo(cx, cy, cx, cy - r)
                close()
            }
            drawPath(path, color = color, style = stroke)
            drawCircle(color = color, radius = 3.5f, center = Offset(cx + r * 0.7f, cy - r * 0.6f))
        }
        1 -> {
            // Crescent moon
            val moon = Path().apply {
                addArc(
                    oval = androidx.compose.ui.geometry.Rect(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f),
                    startAngleDegrees = 45f,
                    sweepAngleDegrees = 230f
                )
                quadraticTo(cx - r * 0.1f, cy, cx + r * 0.5f, cy + r * 0.5f)
            }
            drawPath(moon, color = color, style = stroke)
        }
        else -> {
            // Saturn planet with ring
            drawCircle(color = color, radius = r * 0.46f, center = Offset(cx, cy), style = stroke)
            rotate(-22f, pivot = Offset(cx, cy)) {
                drawOval(
                    color = color,
                    topLeft = Offset(cx - r * 0.95f, cy - r * 0.24f),
                    size = Size(r * 1.9f, r * 0.48f),
                    style = stroke
                )
            }
        }
    }
}

private fun DrawScope.drawFloralLeaf(cx: Float, cy: Float, r: Float, angle: Float, color: Color) {
    rotate(angle, pivot = Offset(cx, cy)) {
        val stroke = Stroke(width = 2.2f, cap = StrokeCap.Round)
        val leaf = Path().apply {
            moveTo(cx, cy + r * 0.85f)
            cubicTo(cx - r * 0.9f, cy + r * 0.1f, cx - r * 0.6f, cy - r * 0.7f, cx, cy - r * 0.9f)
            cubicTo(cx + r * 0.6f, cy - r * 0.7f, cx + r * 0.9f, cy + r * 0.1f, cx, cy + r * 0.85f)
        }
        drawPath(leaf, color = color, style = stroke)
        drawLine(color = color, start = Offset(cx, cy + r * 0.95f), end = Offset(cx, cy - r * 0.6f), strokeWidth = 2f)
        drawLine(color = color, start = Offset(cx, cy + r * 0.2f), end = Offset(cx - r * 0.35f, cy - r * 0.1f), strokeWidth = 1.6f)
        drawLine(color = color, start = Offset(cx, cy - r * 0.15f), end = Offset(cx + r * 0.32f, cy - r * 0.4f), strokeWidth = 1.6f)
    }
}

private fun DrawScope.drawGeometricHeart(cx: Float, cy: Float, r: Float, angle: Float, color: Color) {
    rotate(angle, pivot = Offset(cx, cy)) {
        val stroke = Stroke(width = 2.2f, cap = StrokeCap.Round)
        val heart = Path().apply {
            moveTo(cx, cy + r * 0.85f)
            lineTo(cx - r * 0.88f, cy - r * 0.08f)
            lineTo(cx - r * 0.44f, cy - r * 0.68f)
            lineTo(cx, cy - r * 0.25f)
            lineTo(cx + r * 0.44f, cy - r * 0.68f)
            lineTo(cx + r * 0.88f, cy - r * 0.08f)
            close()
        }
        drawPath(heart, color = color, style = stroke)
        drawLine(color = color, start = Offset(cx, cy - r * 0.25f), end = Offset(cx, cy + r * 0.82f), strokeWidth = 1.5f)
    }
}

private fun DrawScope.drawRoyalMandala(cx: Float, cy: Float, r: Float, color: Color) {
    val stroke = Stroke(width = 1.9f, cap = StrokeCap.Round)
    drawCircle(color = color, radius = r * 0.28f, center = Offset(cx, cy), style = stroke)
    drawCircle(color = color, radius = r * 0.88f, center = Offset(cx, cy), style = stroke)
    for (i in 0 until 8) {
        val rad = (i * 45.0) * PI / 180.0
        val px = cx + (r * 0.56f * cos(rad)).toFloat()
        val py = cy + (r * 0.56f * sin(rad)).toFloat()
        drawCircle(color = color, radius = r * 0.24f, center = Offset(px, py), style = stroke)
    }
}

private fun DrawScope.drawOceanRipples(cx: Float, cy: Float, r: Float, color: Color) {
    val stroke = Stroke(width = 2.1f, cap = StrokeCap.Round)
    drawCircle(color = color, radius = r * 0.25f, center = Offset(cx, cy), style = stroke)
    drawCircle(color = color, radius = r * 0.55f, center = Offset(cx, cy), style = stroke)
    drawCircle(color = color, radius = r * 0.85f, center = Offset(cx, cy), style = stroke)
    val wave = Path().apply {
        moveTo(cx - r, cy + r * 0.95f)
        quadraticTo(cx - r * 0.5f, cy + r * 0.65f, cx, cy + r * 0.95f)
        quadraticTo(cx + r * 0.5f, cy + r * 1.25f, cx + r, cy + r * 0.95f)
    }
    drawPath(wave, color = color, style = stroke)
}

private fun DrawScope.drawCyberCircuit(cx: Float, cy: Float, r: Float, index: Int, color: Color) {
    val strokeWidth = 2.2f
    val path = Path().apply {
        if (index % 2 == 0) {
            moveTo(cx - r * 0.8f, cy - r * 0.4f)
            lineTo(cx - r * 0.2f, cy - r * 0.4f)
            lineTo(cx + r * 0.2f, cy + r * 0.35f)
            lineTo(cx + r * 0.8f, cy + r * 0.35f)
        } else {
            moveTo(cx - r * 0.6f, cy + r * 0.5f)
            lineTo(cx, cy + r * 0.5f)
            lineTo(cx + r * 0.4f, cy - r * 0.4f)
        }
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    drawCircle(color = color, radius = 4.5f, center = Offset(cx - r * 0.8f, cy - r * 0.4f))
    drawCircle(color = color, radius = 4.5f, center = Offset(cx + r * 0.8f, cy + r * 0.35f))
    drawRect(
        color = color,
        topLeft = Offset(cx - r * 0.14f, cy - r * 0.14f),
        size = Size(r * 0.28f, r * 0.28f),
        style = Stroke(width = 1.8f)
    )
}

private fun DrawScope.drawMusicBeat(cx: Float, cy: Float, r: Float, angle: Float, color: Color) {
    rotate(angle * 0.4f, pivot = Offset(cx, cy)) {
        val stroke = Stroke(width = 2.3f, cap = StrokeCap.Round)
        // Beamed eighth notes + equalizer bars
        drawCircle(color = color, radius = r * 0.22f, center = Offset(cx - r * 0.42f, cy + r * 0.45f), style = stroke)
        drawCircle(color = color, radius = r * 0.22f, center = Offset(cx + r * 0.32f, cy + r * 0.3f), style = stroke)
        drawLine(color = color, start = Offset(cx - r * 0.22f, cy + r * 0.45f), end = Offset(cx - r * 0.22f, cy - r * 0.55f), strokeWidth = 2.3f)
        drawLine(color = color, start = Offset(cx + r * 0.52f, cy + r * 0.3f), end = Offset(cx + r * 0.52f, cy - r * 0.7f), strokeWidth = 2.3f)
        drawLine(color = color, start = Offset(cx - r * 0.22f, cy - r * 0.55f), end = Offset(cx + r * 0.52f, cy - r * 0.7f), strokeWidth = 3f)
    }
}

private fun DrawScope.drawButterfly(cx: Float, cy: Float, r: Float, angle: Float, color: Color) {
    rotate(angle, pivot = Offset(cx, cy)) {
        val stroke = Stroke(width = 2.1f, cap = StrokeCap.Round)
        // Left wings
        val leftTop = Path().apply {
            moveTo(cx, cy)
            cubicTo(cx - r * 1.05f, cy - r * 0.9f, cx - r * 0.95f, cy + r * 0.1f, cx, cy + r * 0.05f)
        }
        val leftBottom = Path().apply {
            moveTo(cx, cy + r * 0.05f)
            cubicTo(cx - r * 0.8f, cy + r * 0.35f, cx - r * 0.45f, cy + r * 0.95f, cx, cy + r * 0.45f)
        }
        // Right wings
        val rightTop = Path().apply {
            moveTo(cx, cy)
            cubicTo(cx + r * 1.05f, cy - r * 0.9f, cx + r * 0.95f, cy + r * 0.1f, cx, cy + r * 0.05f)
        }
        val rightBottom = Path().apply {
            moveTo(cx, cy + r * 0.05f)
            cubicTo(cx + r * 0.8f, cy + r * 0.35f, cx + r * 0.45f, cy + r * 0.95f, cx, cy + r * 0.45f)
        }
        drawPath(leftTop, color = color, style = stroke)
        drawPath(leftBottom, color = color, style = stroke)
        drawPath(rightTop, color = color, style = stroke)
        drawPath(rightBottom, color = color, style = stroke)
        drawLine(color = color, start = Offset(cx, cy - r * 0.3f), end = Offset(cx, cy + r * 0.55f), strokeWidth = 2.2f)
    }
}
