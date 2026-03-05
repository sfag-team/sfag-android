package com.sfag.automata.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import com.sfag.R
import com.sfag.automata.EDGE_LINE_WIDTH
import com.sfag.automata.NODE_INNER_OUTLINE_WIDTH
import com.sfag.automata.NODE_INNER_RADIUS
import com.sfag.automata.NODE_OUTLINE_WIDTH
import com.sfag.automata.NODE_RADIUS
import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.machine.State
import com.sfag.shared.ui.component.DefaultDialogWindow
import com.sfag.shared.ui.component.DefaultTextField
import com.sfag.shared.ui.component.ItemSpecificationIcon

/** Renders all states as draggable/tappable nodes. */
@Composable
internal fun Machine.States(
    positions: Map<Int, Offset>,
    modifier: Modifier,
    currentEditingState: EditTools? = null,
    offsetY: Float,
    offsetX: Float,
    borderColor: Color = MaterialTheme.colorScheme.onSurface,
    currentStateFillColor: Color = MaterialTheme.colorScheme.primaryContainer,
    currentStateTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onStateClick: (State) -> Unit,
    onStateDrag: (stateIndex: Int, delta: Offset) -> Unit = { _, _ -> },
    recompose: () -> Unit = {}
) {
    val densityValue = LocalDensity.current.density

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLowest
    states.forEach { state ->
        val statePos = positions[state.index] ?: return@forEach
        Box(
            modifier = Modifier
                .size(NODE_RADIUS.dp)
                .offset(
                    (statePos.x + offsetX / densityValue).dp,
                    (statePos.y + offsetY / densityValue).dp
                )
        ) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .then(
                    if (currentEditingState == EditTools.ADD_STATES) modifier else modifier.pointerInput(
                        Unit
                    ) {
                        if (currentEditingState == EditTools.MOVE) {
                            detectDragGestures(onDrag = { change, dragAmount ->
                                change.consume()
                                val delta = Offset(
                                    dragAmount.x / densityValue,
                                    dragAmount.y / densityValue
                                )
                                onStateDrag(state.index, delta)
                            }, onDragEnd = { recompose() })
                        } else {
                            detectTapGestures {
                                onStateClick(state)
                            }
                        }
                    })
            ) {
                drawCircle(
                    color = borderColor,
                    radius = NODE_RADIUS + 1,
                    style = Stroke(width = NODE_OUTLINE_WIDTH)
                )
                drawCircle(
                    color = if (state.isCurrent) currentStateFillColor else surfaceColor,
                    radius = if (state.isCurrent) NODE_RADIUS - 1 else NODE_RADIUS
                )
                if (state.final) {
                    drawCircle(
                        color = borderColor,
                        radius = NODE_INNER_RADIUS,
                        style = Stroke(width = NODE_INNER_OUTLINE_WIDTH)
                    )
                    drawCircle(
                        color = if (state.isCurrent) currentStateFillColor else surfaceColor,
                        radius = if (state.isCurrent) NODE_INNER_RADIUS - 2 else NODE_INNER_RADIUS - 1
                    )
                }
                if (state.initial) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val arrowTip = cx - (NODE_RADIUS + NODE_OUTLINE_WIDTH / 2f)
                    val headLength = 26f
                    val headHalf = headLength * 0.5f
                    val arrowStart = arrowTip - headLength - NODE_RADIUS

                    drawPath(
                        path = Path().apply {
                            moveTo(arrowStart, cy)
                            lineTo(arrowTip, cy)
                        },
                        color = borderColor,
                        style = Stroke(width = EDGE_LINE_WIDTH, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(arrowTip, cy)
                            lineTo(arrowTip - headLength, cy - headHalf)
                            lineTo(arrowTip - headLength, cy + headHalf)
                            close()
                        },
                        color = borderColor
                    )
                }
            }
            val baseStyle = MaterialTheme.typography.titleMedium
            val scaledFontSize = when {
                state.name.length <= 2 -> baseStyle.fontSize
                state.name.length == 3 -> baseStyle.fontSize * 0.8f
                state.name.length == 4 -> baseStyle.fontSize * 0.65f
                else -> baseStyle.fontSize * 0.55f
            }
            Text(
                text = state.name,
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(modifier),
                style = baseStyle.copy(
                    color = if (state.isCurrent) currentStateTextColor else borderColor,
                    fontSize = scaledFontSize
                ),
                maxLines = 1
            )
        }
    }
}

/** Dialog for creating or editing a state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Machine.AddStateWindow(
    clickOffset: Offset,
    selectedState: State?,
    onPositionAdded: (stateIndex: Int, offset: Offset) -> Unit,
    finished: () -> Unit
) {
    var name by remember {
        mutableStateOf(selectedState?.name ?: "")
    }
    var initial by remember {
        mutableStateOf(selectedState?.initial ?: false)
    }
    var final by remember {
        mutableStateOf(selectedState?.final ?: false)
    }
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    var tooltipMsg by remember { mutableIntStateOf(R.string.duplicate_state_name) }

    DefaultDialogWindow(
        title = null,
        conditionToEnable = name.isNotEmpty(),
        onDismiss = finished,
        onConfirm = {
            val isDuplicate = states.any { it.name == name && it.index != selectedState?.index }
            if (isDuplicate) {
                tooltipMsg = R.string.duplicate_state_name
                scope.launch { tooltipState.show() }
                return@DefaultDialogWindow
            }
            if (selectedState == null) {
                val newIndex = findNewStateIndex()
                addNewState(
                    State(
                        name = name,
                        isCurrent = false,
                        index = newIndex,
                        final = final,
                        initial = initial,
                    )
                )
                onPositionAdded(newIndex, clickOffset)
            } else {
                selectedState.name = name
                selectedState.initial = initial
                selectedState.final = final
            }
            finished()
        }) {

        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(stringResource(tooltipMsg)) } },
            state = tooltipState
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = CenterVertically
            ) {
                ItemSpecificationIcon(
                    icon = R.drawable.initial_state,
                    text = stringResource(R.string.initial_label),
                    isActive = initial
                ) {
                    if (selectedState?.initial == true || !states.any { it.initial }) {
                        initial = !initial
                    } else {
                        tooltipMsg = R.string.initial_state_exists
                        scope.launch { tooltipState.show() }
                    }
                }

                ItemSpecificationIcon(
                    icon = R.drawable.final_state,
                    text = stringResource(R.string.final_label),
                    isActive = final
                ) {
                    final = !final
                }
            }
        }

        DefaultTextField(
            hint = stringResource(R.string.state_name),
            value = name,
            modifier = Modifier.fillMaxWidth(),
            onTextChange = { if (it.length <= 5) name = it })
    }
}

private fun Machine.findNewStateIndex(): Int {
    val sortedStates = states.sortedBy { it.index }
    return if (states.isEmpty()) 1 else if (sortedStates.last().index == sortedStates.size) sortedStates.last().index + 1 else {
        var previousItem = 0
        var selectedIndex = sortedStates.size + 1
        sortedStates.forEach { item ->
            if (item.index == previousItem + 1) {
                previousItem = item.index
            } else {
                selectedIndex = previousItem + 1
            }
        }
        selectedIndex
    }
}
