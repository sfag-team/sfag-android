package com.sfag.automata.ui.common

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

const val NODE_RADIUS = 64f
const val NODE_OUTLINE = NODE_RADIUS * 0.125f

fun fontScaleFactor(nameLength: Int): Float =
    when {
        nameLength <= 2 -> 1f
        nameLength == 3 -> 0.8f
        nameLength == 4 -> 0.65f
        else -> 0.55f
    }

/**
 * Draws a single automaton node: outline circle, filled interior, optional
 * final-state ring, and centered name label.
 *
 * Outline is centered on NODE_RADIUS - outer edge at NODE_RADIUS + NODE_OUTLINE / 2.
 */
fun DrawScope.drawNode(
    center: Offset,
    outlineColor: Color,
    fillColor: Color,
    textArgb: Int,
    name: String,
    textPaint: Paint,
    baseTextSize: Float,
    isFinal: Boolean = false,
    alpha: Float = 1f,
) {
    drawCircle(
        color = outlineColor,
        radius = NODE_RADIUS,
        center = center,
        style = Stroke(width = NODE_OUTLINE),
        alpha = alpha,
    )
    drawCircle(
        color = fillColor,
        radius = NODE_RADIUS - NODE_OUTLINE / 2,
        center = center,
        alpha = alpha,
    )
    if (isFinal) {
        drawCircle(
            color = outlineColor,
            radius = NODE_RADIUS * 0.75f,
            center = center,
            style = Stroke(width = NODE_OUTLINE / 2),
            alpha = alpha,
        )
    }
    if (name.isNotEmpty()) {
        textPaint.color = textArgb
        textPaint.alpha = (alpha * 255).toInt()
        textPaint.textSize = baseTextSize * fontScaleFactor(name.length)
        drawContext.canvas.nativeCanvas.drawText(
            name,
            center.x,
            center.y + textPaint.textSize / 3f,
            textPaint,
        )
    }
}
