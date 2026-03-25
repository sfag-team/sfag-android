package com.sfag.automata.ui.machine

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.ui.common.NODE_OUTLINE
import com.sfag.automata.ui.common.NODE_RADIUS
import com.sfag.automata.ui.common.drawNode
import com.sfag.main.ui.theme.extendedColorScheme
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val ARROW_HEAD_SIZE = NODE_RADIUS * 0.5f

/** Renders all states as nodes (purely visual - interaction handled by MachineView). */
@Composable
internal fun Machine.StateNodes(
    positions: Map<Int, Offset>,
    offsetX: Float,
    offsetY: Float,
    simulationOutcome: SimulationOutcome? = null,
    borderColor: Color = MaterialTheme.colorScheme.onSurface,
    currentStateFillColor: Color = MaterialTheme.colorScheme.primaryContainer,
    currentStateTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val density = LocalDensity.current
    val pxPerDp = density.density

    val baseTextSize = with(density) { MaterialTheme.typography.titleLarge.fontSize.toPx() }
    val textPaint =
        remember(density) {
            Paint().apply {
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
        }

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val acceptedFill = MaterialTheme.extendedColorScheme.accepted.colorContainer
    val acceptedText = MaterialTheme.extendedColorScheme.accepted.onColorContainer
    val rejectedFill = MaterialTheme.extendedColorScheme.rejected.colorContainer
    val rejectedText = MaterialTheme.extendedColorScheme.rejected.onColorContainer
    val borderArgb = borderColor.toArgb()

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
    ) {
        for (state in states) {
            val position = positions[state.index] ?: continue
            val center = Offset(
                (position.x + NODE_RADIUS / 2f) * pxPerDp,
                (position.y + NODE_RADIUS / 2f) * pxPerDp,
            )

            val fillColor: Color
            val textArgb: Int
            if (state.isCurrent && simulationOutcome != null) {
                when (simulationOutcome) {
                    SimulationOutcome.ACCEPTED ->
                        if (state.final) {
                            fillColor = acceptedFill
                            textArgb = acceptedText.toArgb()
                        } else {
                            fillColor = rejectedFill
                            textArgb = rejectedText.toArgb()
                        }

                    SimulationOutcome.REJECTED -> {
                        fillColor = rejectedFill
                        textArgb = rejectedText.toArgb()
                    }

                    SimulationOutcome.ACTIVE,
                    SimulationOutcome.DEAD,
                        -> {
                        fillColor = currentStateFillColor
                        textArgb = currentStateTextColor.toArgb()
                    }
                }
            } else if (state.isCurrent) {
                fillColor = currentStateFillColor
                textArgb = currentStateTextColor.toArgb()
            } else {
                fillColor = surfaceColor
                textArgb = borderArgb
            }

            drawNode(
                center = center,
                outlineColor = borderColor,
                fillColor = fillColor,
                textArgb = textArgb,
                name = state.name,
                textPaint = textPaint,
                baseTextSize = baseTextSize,
                isFinal = state.final,
            )

            if (state.initial) {
                drawInitialArrow(center, borderColor)
            }
        }
    }
}

private fun DrawScope.drawInitialArrow(center: Offset, color: Color) {
    val arrowTip = center.x - NODE_RADIUS
    val arrowStart = arrowTip - NODE_RADIUS * 1.5f
    drawLine(
        color = color,
        start = Offset(arrowStart, center.y),
        end = Offset(arrowTip, center.y),
        strokeWidth = NODE_OUTLINE / 2,
        cap = StrokeCap.Round,
    )
    val halfBase = ARROW_HEAD_SIZE / sqrt(3f)
    val headPath = Path().apply {
        moveTo(arrowTip, center.y)
        lineTo(arrowTip - ARROW_HEAD_SIZE, center.y - halfBase)
        lineTo(arrowTip - ARROW_HEAD_SIZE, center.y + halfBase)
        close()
    }
    drawPath(path = headPath, color = color)
}
