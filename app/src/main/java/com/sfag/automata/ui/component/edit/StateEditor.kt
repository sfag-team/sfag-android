package com.sfag.automata.ui.component.edit

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.machine.EditMachineStates
import com.sfag.R
import com.sfag.automata.domain.model.NODE_OUTLINE_WIDTH
import com.sfag.automata.domain.model.NODE_RADIUS
import com.sfag.automata.ui.component.EDGE_LINE_WIDTH
import com.sfag.automata.ui.component.NODE_INNER_OUTLINE_WIDTH
import com.sfag.automata.ui.component.NODE_INNER_RADIUS
import com.sfag.automata.ui.component.NODE_TEXT_SIZE
import com.sfag.shared.ui.component.DefaultDialogWindow
import com.sfag.automata.ui.component.widget.DefaultTextField
import androidx.compose.ui.res.stringResource
import com.sfag.automata.ui.component.widget.ItemSpecificationIcon

/** Renders all states as draggable/tappable nodes. */
@Composable
internal fun Machine.States(
    positions: Map<Int, Offset>,
    @SuppressLint("ModifierParameter") dragModifier: Modifier,
    currentEditingState: EditMachineStates? = null,
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
                    if (currentEditingState == EditMachineStates.ADD_STATES) dragModifier else dragModifier.pointerInput(
                        Unit
                    ) {
                        if (currentEditingState == EditMachineStates.MOVE) {
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
            Text(
                text = state.name,
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(dragModifier),
                style = MaterialTheme.typography.titleMedium.copy(color = if (state.isCurrent) currentStateTextColor else borderColor)
            )
        }
    }
}


/** Dialog for creating or editing a state. */
@Composable
internal fun Machine.AddStateWindow(
    clickOffset: Offset,
    chosedState: State?,
    onPositionAdded: (stateIndex: Int, offset: Offset) -> Unit,
    finished: () -> Unit
) {
    var name by remember {
        mutableStateOf(chosedState?.name ?: "")
    }
    var initial by remember {
        mutableStateOf(chosedState?.initial ?: false)
    }
    var final by remember {
        mutableStateOf(chosedState?.final ?: false)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DefaultDialogWindow(
        title = null,
        conditionToEnable = name.isNotEmpty(),
        onDismiss = finished,
        onConfirm = {
            if (chosedState == null) {
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
                chosedState.name = name
                chosedState.initial = initial
                chosedState.final = final
            }
            finished()
        }) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = CenterVertically
        ) {
            val initialStateMsg = stringResource(R.string.initial_state_exists)
            ItemSpecificationIcon(
                icon = R.drawable.initial_state,
                text = stringResource(R.string.initial),
                isActive = initial
            ) {
                if (chosedState?.initial == true || !states.any { it.initial }) {
                    initial = !initial
                    errorMessage = null
                } else {
                    errorMessage = initialStateMsg
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

        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        DefaultTextField(
            hint = stringResource(R.string.name_label),
            value = name,
            modifier = Modifier.fillMaxWidth(),
            onTextChange = { name = it })
    }
}

private fun Machine.findNewStateIndex(): Int {
    val sortedStates = states.sortedBy { it.index }
    return if (states.isEmpty()) 1 else if (sortedStates.last().index == sortedStates.size) sortedStates.last().index + 1 else {
        var previousItem = 0
        var choosedIndex = sortedStates.size + 1
        sortedStates.forEach { item ->
            if (item.index == previousItem + 1) {
                previousItem = item.index
            } else {
                choosedIndex = previousItem + 1
            }
        }
        choosedIndex
    }
}
