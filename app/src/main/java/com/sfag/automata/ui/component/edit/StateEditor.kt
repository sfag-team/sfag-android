package com.sfag.automata.ui.component.edit

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.presentation.model.EditMachineStates
import com.sfag.R
import com.sfag.automata.ui.component.common.DefaultDialogWindow
import com.sfag.automata.ui.component.common.DefaultTextField
import com.sfag.automata.ui.component.common.ItemSpecificationIcon
import kotlin.math.roundToInt


/**
 * private compose function States
 *
 * composes all states of machine on the screen
 * @param dragModifier needed for correct drag and drop of states
 *
 */
@Composable
internal fun Machine.States(
    @SuppressLint("ModifierParameter") dragModifier: Modifier,
    currentEditingState: EditMachineStates? = null,
    offsetY: Float,
    offsetX: Float,
    borderColor: Color = MaterialTheme.colorScheme.tertiary,
    onStateClick: (State) -> Unit,
    recompose: () -> Unit = {}
) {

    val currentCircleColor = MaterialTheme.colorScheme.primaryContainer
    states.forEach { state ->
        Box(
            modifier = Modifier
                .size(state.radius.dp)
                .offset(state.position.x.dp, state.position.y.dp)
        ) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .then(
                    if (currentEditingState == EditMachineStates.ADD_STATES) dragModifier else dragModifier.pointerInput(
                        Unit
                    ) {
                        if (currentEditingState == EditMachineStates.MOVE) {
                            detectDragGestures(onDrag = { change, dragAmount ->
                                change.consume()
                                val localOffset = getDpOffsetWithPxOffset(
                                    Offset(
                                        dragAmount.x,
                                        dragAmount.y
                                    )
                                ) ?: return@detectDragGestures
                                state.setX(state.position.x + localOffset.x)
                                state.setY(state.position.y + localOffset.y)


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
                    radius = state.radius + 1,
                    style = Stroke(width = 10f)
                )
                drawCircle(
                    color = if (state.isCurrent) currentCircleColor else Color.White,
                    radius = if (state.isCurrent) state.radius - 1 else state.radius
                )
                if (state.finite) {
                    drawCircle(
                        color = borderColor,
                        radius = state.radius - 10,
                        style = Stroke(width = 5f)
                    )
                    drawCircle(
                        color = if (state.isCurrent) currentCircleColor else Color.White,
                        radius = if (state.isCurrent) state.radius - 12 else state.radius - 11
                    )
                }
                if (state.initial) {
                    val offset = 30f
                    val scaleFactor = 3f

                    val arrowPath = Path().apply {
                        moveTo(size.width * 0.1f - offset * 2, size.height / 2)
                        lineTo(size.width * 0.4f - offset, size.height / 2)
                        lineTo(
                            size.width * 0.35f - offset * 2,
                            size.height / 2 - size.height * 0.05f * scaleFactor
                        )
                        moveTo(size.width * 0.4f - offset, size.height / 2)
                        lineTo(
                            size.width * 0.35f - offset * 2,
                            size.height / 2 + size.height * 0.05f * scaleFactor
                        )
                    }

                    drawPath(
                        path = arrowPath,
                        color = Color.Black,
                        style = Stroke(width = 5f)
                    )
                }
            }
            Text(
                text = state.name,
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(dragModifier)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
                style = TextStyle(color = MaterialTheme.colorScheme.tertiary),
                fontSize = 20.sp
            )
        }
    }
}


/**
 * composes AddStateWindow
 *
 * Shows window to create/edit states
 * @param clickOffset - provides coordinates of click, where should be created new state
 * @param finished - lambda that invokes when user confirm his changes
 */
@Composable
internal fun Machine.AddStateWindow(clickOffset: Offset, chosedState: State?, finished: () -> Unit) {

    var name by remember {
        mutableStateOf(chosedState?.name ?: "")
    }
    var initial by remember {
        mutableStateOf(chosedState?.initial ?: false)
    }
    var finite by remember {
        mutableStateOf(chosedState?.finite ?: false)
    }

    DefaultDialogWindow(
        title = if (chosedState == null) stringResource(id = R.string.new_state) else "edit state: ${chosedState.name}",
        height = 400,
        conditionToEnable = name.isNotEmpty(),
        onDismiss = {
            finished()
        },
        onConfirm = {
            if (chosedState == null) {
                addNewState(
                    State(
                        name = name,
                        isCurrent = false,
                        index = findNewStateIndex(),
                        finite = finite,
                        initial = initial,
                        position = clickOffset
                    )
                )
            } else {
                chosedState.name = name
                chosedState.initial = initial
                chosedState.finite = finite
            }
            finished()
        }) {

        /**
         * Row for initial and finite state
         */
        /**
         * Row for initial and finite state
         */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ItemSpecificationIcon(
                icon = R.drawable.initial_state_icon,
                text = "initial",
                isActive = initial
            ) {
                if (chosedState != null || checkMachineForExistingInitialState()) initial =
                    !initial
            }

            ItemSpecificationIcon(
                icon = R.drawable.finite_state_icon,
                text = "finite",
                isActive = finite
            ) {
                if (chosedState != null || checkMachineForExistingFiniteState()) finite =
                    !finite
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalAlignment = CenterVertically
        ) {
            Text(text = "x: ${clickOffset.x}", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "y: ${clickOffset.y}", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalAlignment = CenterVertically
        ) {
            DefaultTextField(
                hint = "name",
                value = name,
                requirementText = "",
                onTextChange = { name = it }) { true }
        }
    }
}

private fun Machine.checkMachineForExistingFiniteState(): Boolean {
    return if (states.any { it.finite }) {
        Toast.makeText(context, "Your machine already has finite state", Toast.LENGTH_SHORT)
            .show()
        false
    } else true
}

private fun Machine.checkMachineForExistingInitialState(): Boolean {
    return if (states.any { it.initial }) {
        Toast.makeText(
            context,
            "Your machine already has initial state",
            Toast.LENGTH_SHORT
        )
            .show()
        false
    } else true
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
        return choosedIndex
    }
}
