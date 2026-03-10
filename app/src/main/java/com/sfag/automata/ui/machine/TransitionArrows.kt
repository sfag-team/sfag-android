package com.sfag.automata.ui.machine

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.Transition
import com.sfag.automata.ui.common.EDGE_LINE_WIDTH
import com.sfag.automata.ui.common.EDGE_TEXT_SIZE
import com.sfag.automata.ui.common.NODE_RADIUS
import kotlin.math.roundToInt
import kotlin.math.sqrt

private data class GroupedArrow(
    val path: TransitionPath,
    val labels: List<String>,
)

/** Draws all transitions as tappable bezier arrows. */
@Composable
internal fun Machine.TransitionArrows(
    positions: Map<Int, Offset>,
    modifier: Modifier,
    offsetX: Float,
    offsetY: Float,
    onClickTransition: ((Transition) -> Unit)?,
    borderColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val density = LocalDensity.current
    val colorOnSurface = MaterialTheme.colorScheme.onSurface
    val transitionPaths = computePaths(positions, density)

    val scaledTextSize = EDGE_TEXT_SIZE * density.density
    val labelPaint =
        remember(density, colorOnSurface) {
            Paint().apply {
                color = colorOnSurface.toArgb()
                textSize = scaledTextSize
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
        }

    // Group by (fromState, toState) to stack labels for same-pair transitions on one arrow
    val grouped =
        transitions.withIndex().groupBy { (_, transition) ->
            Pair(transition.fromState, transition.toState)
        }

    val groupedArrows =
        grouped.mapNotNull { (_, indexedTransitions) ->
            val firstIndex = indexedTransitions.first().index
            val transitionPath = transitionPaths.getOrNull(firstIndex) ?: return@mapNotNull null
            GroupedArrow(
                path = transitionPath,
                labels = indexedTransitions.map { (_, transition) -> transition.displayLabel() },
            )
        }

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .then(
                    if (onClickTransition == null) {
                        modifier
                    } else {
                        modifier.pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val adjustedX = tapOffset.x - offsetX
                                val adjustedY = tapOffset.y - offsetY
                                for ((index, transitionPath) in transitionPaths.withIndex()) {
                                    if (
                                        transitionPath != null &&
                                        isTransitionHit(transitionPath, adjustedX, adjustedY)
                                    ) {
                                        onClickTransition(transitions[index])
                                        break
                                    }
                                }
                            }
                        }
                    },
                ).offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
    ) {
        // Pass 1: Draw all arrow bodies
        for (arrow in groupedArrows) {
            drawPath(
                arrow.path.bodyPath,
                color = borderColor,
                style = Stroke(width = EDGE_LINE_WIDTH, cap = StrokeCap.Round),
            )
        }

        // Pass 2: Draw all arrow heads (filled triangle)
        for (arrow in groupedArrows) {
            drawPath(arrow.path.headPath, color = borderColor)
        }

        // Pass 3: Draw all labels
        val lineHeight = scaledTextSize * 1.25f
        val selfLoopClearance = scaledTextSize * 0.25f + EDGE_LINE_WIDTH
        val normalClearance = scaledTextSize * 0.5f + EDGE_LINE_WIDTH

        for (arrow in groupedArrows) {
            val labelClearance = if (arrow.path.isSelfLoop) selfLoopClearance else normalClearance
            val count = arrow.labels.size

            val maxTextWidth = arrow.labels.maxOf { labelPaint.measureText(it) }
            val halfW = (maxTextWidth * 0.5f).coerceAtLeast(1f)
            val halfH = (count * scaledTextSize * 0.625f).coerceAtLeast(1f)

            val deltaX = arrow.path.textPosition.x - arrow.path.controlPoint.x
            val deltaY = arrow.path.textPosition.y - arrow.path.controlPoint.y
            val deltaLength = sqrt(deltaX * deltaX + deltaY * deltaY)
            val normalX = if (deltaLength > 0.1f) deltaX / deltaLength else 0f
            val normalY = if (deltaLength > 0.1f) deltaY / deltaLength else 1f

            val normalXScaled = normalX / halfW
            val normalYScaled = normalY / halfH
            val denominatorSq = normalXScaled * normalXScaled + normalYScaled * normalYScaled
            val edgeDist = if (denominatorSq > 0f) 1f / sqrt(denominatorSq) else halfH

            val adjustment = edgeDist + labelClearance - NODE_RADIUS * 2f
            val centerX = arrow.path.textPosition.x + normalX * adjustment
            val centerY = arrow.path.textPosition.y + normalY * adjustment

            val baseY = centerY - (count - 1) * lineHeight * 0.5f + scaledTextSize * 0.375f
            arrow.labels.forEachIndexed { i, label ->
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    centerX,
                    baseY + i * lineHeight,
                    labelPaint,
                )
            }
        }
    }
}

private fun isTransitionHit(
    transitionPath: TransitionPath,
    tapX: Float,
    tapY: Float,
    threshold: Float = 40f,
): Boolean {
    val controlPoint = transitionPath.controlPoint
    val dx = tapX - controlPoint.x
    val dy = tapY - controlPoint.y
    return (dx * dx + dy * dy) < threshold * threshold
}
