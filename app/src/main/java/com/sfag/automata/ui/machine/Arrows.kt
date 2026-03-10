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
import com.sfag.automata.ui.common.NODE_RADIUS
import kotlin.math.roundToInt
import kotlin.math.sqrt

private data class GroupedArrow(
    val path: ArrowPath,
    val labels: List<String>,
)

/** Draws arrow body lines and handles tap detection. */
@Composable
internal fun Machine.ArrowBodies(
    positions: Map<Int, Offset>,
    modifier: Modifier,
    offsetX: Float,
    offsetY: Float,
    onClickTransition: ((Transition) -> Unit)?,
    borderColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val density = LocalDensity.current
    val arrowPaths = computePaths(positions, density)

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
                                for ((index, arrowPath) in arrowPaths.withIndex()) {
                                    if (
                                        arrowPath != null &&
                                        isArrowHit(arrowPath, adjustedX, adjustedY)
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
        for (arrow in groupArrows(arrowPaths)) {
            drawPath(
                arrow.path.bodyPath,
                color = borderColor,
                style = Stroke(width = NODE_RADIUS * 0.125f, cap = StrokeCap.Round),
            )
        }
    }
}

/** Draws arrowheads and labels. */
@Composable
internal fun Machine.ArrowHeads(
    positions: Map<Int, Offset>,
    offsetX: Float,
    offsetY: Float,
    borderColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val density = LocalDensity.current
    val colorOnSurface = MaterialTheme.colorScheme.onSurface
    val arrowPaths = computePaths(positions, density)

    val scaledTextSize = 22f * density.density
    val labelPaint =
        remember(density, colorOnSurface) {
            Paint().apply {
                color = colorOnSurface.toArgb()
                textSize = scaledTextSize
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
        }

    val groupedArrows = groupArrows(arrowPaths)

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
    ) {
        for (arrow in groupedArrows) {
            drawPath(arrow.path.headPath, color = borderColor)
        }

        val lineHeight = scaledTextSize * 1.25f
        val selfLoopClearance = scaledTextSize * 0.25f + NODE_RADIUS * 0.125f
        val normalClearance = scaledTextSize * 0.5f + NODE_RADIUS * 0.125f

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

private fun Machine.groupArrows(arrowPaths: List<ArrowPath?>): List<GroupedArrow> {
    val grouped =
        transitions.withIndex().groupBy { (_, transition) ->
            Pair(transition.fromState, transition.toState)
        }
    return grouped.mapNotNull { (_, indexedTransitions) ->
        val firstIndex = indexedTransitions.first().index
        val arrowPath = arrowPaths.getOrNull(firstIndex) ?: return@mapNotNull null
        GroupedArrow(
            path = arrowPath,
            labels = indexedTransitions.map { (_, transition) -> transition.displayLabel() },
        )
    }
}

private fun isArrowHit(
    arrowPath: ArrowPath,
    tapX: Float,
    tapY: Float,
    threshold: Float = 40f,
): Boolean {
    val thresholdSq = threshold * threshold
    val samples = 8
    for (i in 0..samples) {
        val position = getCurrentPositionByPath(arrowPath.bodyPath, i.toFloat() / samples)
        val dx = tapX - position.x
        val dy = tapY - position.y
        if (dx * dx + dy * dy < thresholdSq) return true
    }
    val dx = tapX - arrowPath.textPosition.x
    val dy = tapY - arrowPath.textPosition.y
    return dx * dx + dy * dy < thresholdSq
}
