package com.sfag.automata.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlin.math.sqrt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sfag.R
import com.sfag.shared.ui.theme.extendedColorScheme
import com.sfag.automata.domain.model.NODE_RADIUS
import com.sfag.automata.domain.model.machine.EditMachineStates
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.machine.PushDownMachine
import com.sfag.automata.domain.model.machine.TuringMachine
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.automata.domain.usecase.isDeterministic
import com.sfag.automata.ui.component.edit.AddStateWindow
import com.sfag.automata.ui.component.edit.CreateTransitionWindow
import com.sfag.automata.ui.component.edit.States
import com.sfag.automata.ui.component.edit.ToolsRow
import com.sfag.automata.ui.component.edit.Transitions
import androidx.compose.foundation.lazy.rememberLazyListState
import com.sfag.automata.ui.component.simulation.InputTapeBar
import com.sfag.automata.ui.component.simulation.PushDownStackBar
import com.sfag.automata.ui.component.simulation.TAPE_BAR_HEIGHT
import com.sfag.automata.ui.component.simulation.TuringTapeBar
import com.sfag.automata.ui.viewmodel.AutomataViewModel

/**
 * Mutable holder for the editor-opening functions defined inside [AutomataView].
 * Passed in by the parent so the bottom panel can trigger the same dialogs.
 */
class EditActions {
    var openState: (State) -> Unit = {}
    var openTransition: (Transition) -> Unit = {}
}

/**
 * Unified view for both simulation and editing modes.
 */
@SuppressLint("ComposableNaming")
@Composable
fun Machine.AutomataView(
    isEditing: Boolean,
    recomposeKey: Int,
    onEditInputClick: () -> Unit,
    increaseRecomposeValue: () -> Unit,
    animationOverlay: (@Composable () -> Unit)? = null,
    editActions: EditActions? = null,
    onNameClick: (() -> Unit)? = null,
    simulationEndResult: Boolean? = null,
) {
    val viewModel: AutomataViewModel = hiltViewModel(LocalActivity.current as ComponentActivity)
    val density = LocalDensity.current

    var editMode by remember { mutableStateOf(EditMachineStates.EDITING) }
    var recomposition by remember { mutableIntStateOf(0) }
    var clickOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var addStateWindowFocused by remember { mutableStateOf(false) }
    var addTransitionWindowFocused by remember { mutableStateOf(false) }
    var choosedStateForTransitionStart by remember { mutableStateOf<State?>(null) }
    var choosedStateForTransitionEnd by remember { mutableStateOf<State?>(null) }
    var choosedTransitionName by remember { mutableStateOf<String?>(null) }
    var push by remember { mutableStateOf<String?>(null) }
    var pop by remember { mutableStateOf<String?>(null) }
    var chosedStateForEditing by remember { mutableStateOf<State?>(null) }

    val openStateForEditing: (State) -> Unit = { state ->
        chosedStateForEditing = state
        addStateWindowFocused = true
    }

    val openTransitionForEditing: (Transition) -> Unit = { transition ->
        choosedTransitionName = transition.name
        if (machineType == MachineType.Pushdown) {
            transition as PushDownTransition
            push = transition.push
            pop = transition.pop
        }
        choosedStateForTransitionStart = getStateByIndex(transition.startState)
        choosedStateForTransitionEnd = getStateByIndex(transition.endState)
        addTransitionWindowFocused = true
    }

    // Register current lambdas so the bottom panel can trigger the same dialogs.
    editActions?.openState = openStateForEditing
    editActions?.openTransition = openTransitionForEditing

    val tapeListState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP BAR
        if (isEditing) {
            key(editMode) {
                ToolsRow(editMode = editMode) { newMode ->
                    editMode = newMode
                }
            }
        } else {
            key(recomposeKey) {
                when (machineType) {
                    MachineType.Turing -> (this@AutomataView as TuringMachine).TuringTapeBar(
                        listState = tapeListState,
                        onEditClick = onEditInputClick
                    )
                    MachineType.Finite, MachineType.Pushdown -> InputTapeBar(
                        listState = tapeListState,
                        onEditClick = onEditInputClick
                    )
                }
            }
        }

        // CENTER: State diagram with zoom/pan
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(1.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            // Auto-fit on first load
            if (viewModel.needsAutoFit && states.isNotEmpty() && viewModel.statePositions.isNotEmpty()) {
                val viewW = maxWidth.value
                val viewH = maxHeight.value

                val positions = viewModel.statePositions
                val minX = positions.values.minOf { it.x }
                val maxX = positions.values.maxOf { it.x }
                val minY = positions.values.minOf { it.y }
                val maxY = positions.values.maxOf { it.y }

                val nodeSize = NODE_RADIUS
                val padding = nodeSize * 2
                val contentW = maxX - minX + nodeSize + padding * 2
                val contentH = maxY - minY + nodeSize + padding * 2

                val fitScale = minOf(viewW / contentW, viewH / contentH).coerceIn(0.2f, 0.75f)
                val centroidX = (minX + maxX + nodeSize) / 2
                val centroidY = (minY + maxY + nodeSize) / 2

                viewModel.scaleGraph = fitScale
                viewModel.offsetXGraph = density.density * (viewW / (2 * fitScale) - centroidX)
                viewModel.offsetYGraph = density.density * (viewH / (2 * fitScale) - centroidY)
                viewModel.needsAutoFit = false
            }

            var offsetX by remember { mutableFloatStateOf(viewModel.offsetXGraph) }
            var offsetY by remember { mutableFloatStateOf(viewModel.offsetYGraph) }
            var scale by remember { mutableFloatStateOf(viewModel.scaleGraph) }

            val gestureModifier = Modifier.pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(0.3f, 3f)
                    offsetX += centroid.x * (1f / scale - 1f / oldScale) + pan.x / scale
                    offsetY += centroid.y * (1f / scale - 1f / oldScale) + pan.y / scale
                    viewModel.scaleGraph = scale
                    viewModel.offsetXGraph = offsetX
                    viewModel.offsetYGraph = offsetY
                }
            }

            Box(modifier = Modifier
                .fillMaxSize()
                .pointerInput(editMode, isEditing) {
                    if (!isEditing) return@pointerInput
                    when (editMode) {
                        EditMachineStates.ADD_STATES -> detectTapGestures { tapOffset ->
                            clickOffset = Offset(
                                tapOffset.x / (scale * density.density),
                                tapOffset.y / (scale * density.density)
                            )
                            addStateWindowFocused = true
                        }
                        EditMachineStates.ADD_TRANSITIONS -> detectTapGestures { tapOffset ->
                            val tapDpX = tapOffset.x / (scale * density.density)
                            val tapDpY = tapOffset.y / (scale * density.density)
                            val positions = viewModel.statePositions
                            val closest = positions.minByOrNull { (_, pos) ->
                                val dx = pos.x + NODE_RADIUS / 2 - tapDpX
                                val dy = pos.y + NODE_RADIUS / 2 - tapDpY
                                sqrt(dx * dx + dy * dy)
                            }
                            closest?.let { (stateIdx, _) ->
                                val state = getStateByIndex(stateIdx)
                                choosedStateForTransitionStart = state
                                choosedStateForTransitionEnd = state
                                addTransitionWindowFocused = true
                            }
                        }
                        else -> {}
                    }
                }
                .then(gestureModifier)
            ) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                ) {
                    val positions = viewModel.statePositions

                    if (isEditing) {
                        key(editMode) {
                            val dragModifier = Modifier

                            key(recomposeKey, recomposition) {
                                Transitions(
                                    positions = positions,
                                    dragModifier = dragModifier,
                                    offsetY = offsetY, offsetX = offsetX,
                                    onTransitionClick = when (editMode) {
                                        EditMachineStates.EDITING -> openTransitionForEditing
                                        EditMachineStates.DELETE -> { transition ->
                                            deleteTransition(transition)
                                            recomposition++
                                            increaseRecomposeValue()
                                        }
                                        else -> null
                                    }
                                )
                                States(
                                    positions = positions,
                                    dragModifier = dragModifier,
                                    currentEditingState = editMode,
                                    offsetY = offsetY, offsetX = offsetX,
                                    onStateClick = { state ->
                                        when (editMode) {
                                            EditMachineStates.ADD_TRANSITIONS -> {
                                                choosedStateForTransitionStart = state
                                                choosedStateForTransitionEnd = state
                                                addTransitionWindowFocused = true
                                            }
                                            EditMachineStates.DELETE -> {
                                                deleteState(state)
                                                viewModel.statePositions.remove(state.index)
                                                recomposition++
                                            }
                                            EditMachineStates.EDITING -> openStateForEditing(state)
                                            else -> {}
                                        }
                                    },
                                    onStateDrag = { stateIndex, delta ->
                                        viewModel.updateStatePosition(stateIndex, delta)
                                    },
                                    recompose = {
                                        recomposition++
                                        increaseRecomposeValue()
                                    }
                                )
                            }
                        }
                    } else {
                        // Simulation mode - compute state fill/text based on simulation outcome
                        val simFill = when (simulationEndResult) {
                            true -> MaterialTheme.extendedColorScheme.accepted.colorContainer
                            false -> MaterialTheme.extendedColorScheme.rejected.colorContainer
                            null -> MaterialTheme.colorScheme.primaryContainer
                        }
                        val simText = when (simulationEndResult) {
                            true -> MaterialTheme.extendedColorScheme.accepted.onColorContainer
                            false -> MaterialTheme.extendedColorScheme.rejected.onColorContainer
                            null -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                        key(recomposeKey) {
                            Transitions(
                                positions = positions,
                                dragModifier = Modifier, offsetY = offsetY, offsetX = offsetX,
                                onTransitionClick = null
                            )
                        }
                        animationOverlay?.invoke()
                        key(recomposeKey) {
                            States(
                                positions = positions,
                                dragModifier = Modifier, currentEditingState = null,
                                offsetY = offsetY, offsetX = offsetX,
                                currentStateFillColor = simFill,
                                currentStateTextColor = simText,
                                onStateClick = {}
                            )
                        }
                    }
                }
            }

            Text(
                text = name.ifEmpty { stringResource(R.string.untitled) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp, start = 8.dp)
                    .then(
                        if (onNameClick != null) Modifier.clickable { onNameClick() }
                        else Modifier
                    )
            )

            Text(
                text = when (isDeterministic()) {
                    true -> stringResource(R.string.deterministic)
                    false -> stringResource(R.string.non_deterministic)
                    null -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            )

            // Empty state hint
            if (states.isEmpty()) {
                Text(
                    text = stringResource(if (isEditing) R.string.tap_to_add_states else R.string.no_states),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Edit dialogs
            if (isEditing) {
                if (addStateWindowFocused) AddStateWindow(
                    clickOffset, chosedStateForEditing,
                    onPositionAdded = { stateIndex, offset ->
                        viewModel.addStatePosition(stateIndex, offset)
                    }
                ) {
                    addStateWindowFocused = false
                    chosedStateForEditing = null
                    recomposition++
                    increaseRecomposeValue()
                }
                val transitionStart = choosedStateForTransitionStart
                val transitionEnd = choosedStateForTransitionEnd
                if (addTransitionWindowFocused && transitionStart != null && transitionEnd != null) {
                    CreateTransitionWindow(
                        transitionStart,
                        transitionEnd,
                        choosedTransitionName,
                        push,
                        pop
                    ) {
                        addTransitionWindowFocused = false
                        choosedStateForTransitionStart = null
                        choosedStateForTransitionEnd = null
                        choosedTransitionName = null
                        push = null
                        pop = null
                        recomposition++
                        increaseRecomposeValue()
                    }
                }
            }
        }

        // BOTTOM BAR (PDA stack - simulation only)
        if (machineType == MachineType.Pushdown && !isEditing) {
            key(recomposeKey) {
                (this@AutomataView as PushDownMachine).PushDownStackBar()
            }
        }
    }
}
