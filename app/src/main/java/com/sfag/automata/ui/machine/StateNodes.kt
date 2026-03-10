package com.sfag.automata.ui.machine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.State
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.ui.common.EDGE_LINE_WIDTH
import com.sfag.automata.ui.common.NODE_INNER_OUTLINE_WIDTH
import com.sfag.automata.ui.common.NODE_INNER_RADIUS
import com.sfag.automata.ui.common.NODE_OUTLINE_WIDTH
import com.sfag.automata.ui.common.NODE_RADIUS
import com.sfag.main.ui.theme.extendedColorScheme

private const val FONT_SCALE_3_CHAR = 0.8f
private const val FONT_SCALE_4_CHAR = 0.65f
private const val FONT_SCALE_5_PLUS = 0.55f

/** Renders all states as draggable/tappable nodes. */
@Composable
internal fun Machine.StateNodes(
    positions: Map<Int, Offset>,
    modifier: Modifier,
    activeTool: MachineEditMode?,
    offsetX: Float,
    offsetY: Float,
    simulationOutcome: SimulationOutcome? = null,
    borderColor: Color = MaterialTheme.colorScheme.onSurface,
    currentStateFillColor: Color = MaterialTheme.colorScheme.primaryContainer,
    currentStateTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClickState: (State) -> Unit = {},
    onDragState: (stateIndex: Int, delta: Offset) -> Unit = { _, _ -> },
    onRecompose: () -> Unit = {},
) {
    val densityValue = LocalDensity.current.density

    // Pre-compute initial-state arrow paths (same geometry for all states, depends only on density)
    val initialArrowPaths =
        remember(densityValue) {
            val canvasSizePx = NODE_RADIUS * densityValue
            val cx = canvasSizePx / 2f
            val cy = canvasSizePx / 2f
            val arrowTip = cx - (NODE_RADIUS + NODE_OUTLINE_WIDTH / 2f)
            val headLength = 26f
            val headHalf = headLength * 0.5f
            val arrowStart = arrowTip - headLength - NODE_RADIUS
            val linePath =
                Path().apply {
                    moveTo(arrowStart, cy)
                    lineTo(arrowTip, cy)
                }
            val headPath =
                Path().apply {
                    moveTo(arrowTip, cy)
                    lineTo(arrowTip - headLength, cy - headHalf)
                    lineTo(arrowTip - headLength, cy + headHalf)
                    close()
                }
            linePath to headPath
        }

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLowest

    // Per-state outcome colors (NFA: final+current = accepted, non-final+current = rejected)
    val acceptedFill = MaterialTheme.extendedColorScheme.accepted.colorContainer
    val acceptedText = MaterialTheme.extendedColorScheme.accepted.onColorContainer
    val rejectedFill = MaterialTheme.extendedColorScheme.rejected.colorContainer
    val rejectedText = MaterialTheme.extendedColorScheme.rejected.onColorContainer

    states.forEach { state ->
        key(state.index) {
            val statePosition = positions[state.index] ?: return@forEach

            // Resolve fill/text per state based on simulation outcome
            val fillColor: Color
            val textColor: Color
            if (state.isCurrent && simulationOutcome != null) {
                when (simulationOutcome) {
                    SimulationOutcome.ACCEPTED ->
                        if (state.final) {
                            fillColor = acceptedFill
                            textColor = acceptedText
                        } else {
                            fillColor = rejectedFill
                            textColor = rejectedText
                        }
                    SimulationOutcome.REJECTED -> {
                        fillColor = rejectedFill
                        textColor = rejectedText
                    }
                    SimulationOutcome.ACTIVE,
                    SimulationOutcome.DEAD,
                    -> {
                        fillColor = currentStateFillColor
                        textColor = currentStateTextColor
                    }
                }
            } else if (state.isCurrent) {
                fillColor = currentStateFillColor
                textColor = currentStateTextColor
            } else {
                fillColor = surfaceColor
                textColor = borderColor
            }

            Box(
                modifier =
                    Modifier
                        .size(NODE_RADIUS.dp)
                        .offset(
                            (statePosition.x + offsetX / densityValue).dp,
                            (statePosition.y + offsetY / densityValue).dp,
                        ),
            ) {
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (activeTool == MachineEditMode.ADD_STATES) {
                                    modifier
                                } else {
                                    modifier.pointerInput(Unit) {
                                        if (activeTool == MachineEditMode.MOVE) {
                                            detectDragGestures(
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val delta =
                                                        Offset(
                                                            dragAmount.x / densityValue,
                                                            dragAmount.y / densityValue,
                                                        )
                                                    onDragState(state.index, delta)
                                                },
                                                onDragEnd = { onRecompose() },
                                            )
                                        } else {
                                            detectTapGestures { onClickState(state) }
                                        }
                                    }
                                },
                            ),
                ) {
                    drawCircle(
                        color = borderColor,
                        radius = NODE_RADIUS + 1,
                        style = Stroke(width = NODE_OUTLINE_WIDTH),
                    )
                    drawCircle(
                        color = fillColor,
                        radius = if (state.isCurrent) NODE_RADIUS - 1 else NODE_RADIUS,
                    )
                    if (state.final) {
                        drawCircle(
                            color = borderColor,
                            radius = NODE_INNER_RADIUS,
                            style = Stroke(width = NODE_INNER_OUTLINE_WIDTH),
                        )
                        drawCircle(
                            color = fillColor,
                            radius =
                                if (state.isCurrent) {
                                    NODE_INNER_RADIUS - 2
                                } else {
                                    NODE_INNER_RADIUS - 1
                                },
                        )
                    }
                    if (state.initial) {
                        drawPath(
                            path = initialArrowPaths.first,
                            color = borderColor,
                            style = Stroke(width = EDGE_LINE_WIDTH, cap = StrokeCap.Round),
                        )
                        drawPath(path = initialArrowPaths.second, color = borderColor)
                    }
                }
                val baseStyle = MaterialTheme.typography.titleMedium
                val scaledFontSize =
                    when {
                        state.name.length <= 2 -> baseStyle.fontSize
                        state.name.length == 3 -> baseStyle.fontSize * FONT_SCALE_3_CHAR
                        state.name.length == 4 -> baseStyle.fontSize * FONT_SCALE_4_CHAR
                        else -> baseStyle.fontSize * FONT_SCALE_5_PLUS
                    }
                Text(
                    text = state.name,
                    modifier = Modifier.align(Alignment.Center).then(modifier),
                    style = baseStyle.copy(color = textColor, fontSize = scaledFontSize),
                    maxLines = 1,
                )
            }
        }
    }
}
