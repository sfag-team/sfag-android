package com.sfag.automata.ui.machine

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.ui.common.NODE_OUTLINE
import com.sfag.automata.ui.common.NODE_RADIUS
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val ARROW_HEAD_SIZE = NODE_RADIUS * 0.5f

private data class GroupedArrow(
    val path: TransitionPath,
    val labels: List<String>,
)

/** Draws transition arrows: body strokes, arrowheads, and labels in a single canvas. */
@Composable
internal fun Machine.TransitionArrows(
    transitionPaths: List<TransitionPath?>,
    modifier: Modifier,
    offsetX: Float,
    offsetY: Float,
    borderColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val density = LocalDensity.current
    val scaledTextSize = with(density) { MaterialTheme.typography.titleLarge.fontSize.toPx() }
    val labelPaint =
        remember(density, borderColor) {
            Paint().apply {
                color = borderColor.toArgb()
                textSize = scaledTextSize
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
        }

    val groupedArrows = groupArrows(transitionPaths)

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .then(modifier)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
    ) {
        // Body strokes
        for (arrow in groupedArrows) {
            drawPath(
                arrow.path.arrowBody,
                color = borderColor,
                style = Stroke(width = NODE_OUTLINE / 2, cap = StrokeCap.Round),
            )
        }

        // Arrowheads
        for (arrow in groupedArrows) {
            drawPath(buildArrowHead(arrow.path), color = borderColor)
        }

        // Labels
        val lineHeight = scaledTextSize * 1.25f
        val selfLoopClearance = scaledTextSize * 0.25f + NODE_OUTLINE
        val regularClearance = scaledTextSize * 0.5f + NODE_OUTLINE

        for (arrow in groupedArrows) {
            val labelClearance = if (arrow.path.isSelfLoop) selfLoopClearance else regularClearance
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

/** Samples the path at ARROW_HEAD_SIZE back from the tip to get direction, then builds equilateral head. */
private fun buildArrowHead(path: TransitionPath): Path {
    val pathMeasure = android.graphics.PathMeasure(path.arrowBody.asAndroidPath(), false)
    val length = pathMeasure.length
    val point = FloatArray(2)
    pathMeasure.getPosTan((length - ARROW_HEAD_SIZE).coerceAtLeast(0f), point, null)
    val dx = path.tipX - point[0]
    val dy = path.tipY - point[1]
    val dirLen = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    val dirX = dx / dirLen
    val dirY = dy / dirLen
    val baseX = path.tipX - dirX * ARROW_HEAD_SIZE
    val baseY = path.tipY - dirY * ARROW_HEAD_SIZE
    val halfBase = ARROW_HEAD_SIZE / sqrt(3f)
    val perpX = -dirY * halfBase
    val perpY = dirX * halfBase
    return Path().apply {
        moveTo(path.tipX, path.tipY)
        lineTo(baseX - perpX, baseY - perpY)
        lineTo(baseX + perpX, baseY + perpY)
        close()
    }
}

private fun Machine.groupArrows(transitionPaths: List<TransitionPath?>): List<GroupedArrow> {
    val grouped =
        transitions.withIndex().groupBy { (_, transition) ->
            Pair(transition.fromState, transition.toState)
        }
    return grouped.mapNotNull { (_, indexedTransitions) ->
        val firstIndex = indexedTransitions.first().index
        val transitionPath = transitionPaths.getOrNull(firstIndex) ?: return@mapNotNull null
        GroupedArrow(
            path = transitionPath,
            labels = indexedTransitions.map { (_, transition) -> transition.displayLabel() },
        )
    }
}

