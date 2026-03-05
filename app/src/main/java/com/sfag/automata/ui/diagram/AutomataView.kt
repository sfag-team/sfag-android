package com.sfag.automata.ui.diagram

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlin.math.sqrt

import com.sfag.R
import com.sfag.automata.NODE_RADIUS
import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.machine.MachineType
import com.sfag.automata.model.machine.PushdownMachine
import com.sfag.automata.model.machine.State
import com.sfag.automata.model.machine.TuringMachine
import com.sfag.automata.model.machine.isDeterministic
import com.sfag.automata.model.simulation.SimulationOutcome
import com.sfag.automata.model.transition.PushdownTransition
import com.sfag.automata.model.transition.Transition
import com.sfag.automata.ui.AutomataViewModel
import com.sfag.automata.ui.editor.AddStateWindow
import com.sfag.automata.ui.editor.CreateTransitionWindow
import com.sfag.automata.ui.editor.EditTools
import com.sfag.automata.ui.editor.States
import com.sfag.automata.ui.editor.ToolsRow
import com.sfag.automata.ui.editor.Transitions
import com.sfag.automata.ui.panel.BidirectionalTape
import com.sfag.automata.ui.panel.PushdownStack
import com.sfag.automata.ui.panel.UnidirectionalTape
import com.sfag.shared.ui.theme.extendedColorScheme

private enum class ActiveDialog { ADD_STATE, ADD_TRANSITION }

// Auto-fit zoom range (load/create/reset) - keeps nodes readable
private const val AUTO_FIT_MIN_SCALE = 0.5f
private const val AUTO_FIT_MAX_SCALE = 1f

// Manual zoom range (pinch gesture) - wider for user exploration
private const val MANUAL_MIN_SCALE = 0.25f
private const val MANUAL_MAX_SCALE = 2.5f

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
@Composable
fun Machine.AutomataView(
    isEditing: Boolean,
    recomposeKey: Int,
    onEditInputClick: () -> Unit,
    increaseRecomposeValue: () -> Unit,
    animationOverlay: (@Composable () -> Unit)? = null,
    editActions: EditActions? = null,
    onNameClick: (() -> Unit)? = null,
    simulationOutcome: SimulationOutcome? = null,
) {
    val viewModel: AutomataViewModel = hiltViewModel(LocalActivity.current as ComponentActivity)
    val density = LocalDensity.current

    var editMode by remember { mutableStateOf(EditTools.EDITING) }
    var recomposition by remember { mutableIntStateOf(0) }
    var clickOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var activeDialog by remember { mutableStateOf<ActiveDialog?>(null) }
    var chosenStateForTransitionStart by remember { mutableStateOf<State?>(null) }
    var chosenStateForTransitionEnd by remember { mutableStateOf<State?>(null) }
    var chosenTransitionName by remember { mutableStateOf<String?>(null) }
    var push by remember { mutableStateOf<String?>(null) }
    var pop by remember { mutableStateOf<String?>(null) }
    var chosenStateForEditing by remember { mutableStateOf<State?>(null) }

    val openStateForEditing: (State) -> Unit = { state ->
        chosenStateForEditing = state
        activeDialog = ActiveDialog.ADD_STATE
    }

    val openTransitionForEditing: (Transition) -> Unit = { transition ->
        chosenTransitionName = transition.name
        if (machineType == MachineType.Pushdown) {
            transition as PushdownTransition
            push = transition.push
            pop = transition.pop
        }
        chosenStateForTransitionStart = getStateByIndex(transition.startState)
        chosenStateForTransitionEnd = getStateByIndex(transition.endState)
        activeDialog = ActiveDialog.ADD_TRANSITION
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
                    MachineType.Turing -> (this@AutomataView as TuringMachine).BidirectionalTape(
                        listState = tapeListState,
                        onEditClick = onEditInputClick
                    )
                    MachineType.Finite, MachineType.Pushdown -> UnidirectionalTape(
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
            var offsetX by remember { mutableFloatStateOf(viewModel.offsetXGraph) }
            var offsetY by remember { mutableFloatStateOf(viewModel.offsetYGraph) }
            var scale by remember { mutableFloatStateOf(viewModel.scaleGraph) }

            // Auto-fit on first load or reset
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

                val fitScale = minOf(viewW / contentW, viewH / contentH).coerceIn(AUTO_FIT_MIN_SCALE, AUTO_FIT_MAX_SCALE)
                val centroidX = (minX + maxX + nodeSize) / 2
                val centroidY = (minY + maxY + nodeSize) / 2

                scale = fitScale
                offsetX = density.density * (viewW / (2 * fitScale) - centroidX)
                offsetY = density.density * (viewH / (2 * fitScale) - centroidY)
                viewModel.scaleGraph = scale
                viewModel.offsetXGraph = offsetX
                viewModel.offsetYGraph = offsetY
                viewModel.needsAutoFit = false
            }

            val gestureModifier = Modifier.pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(MANUAL_MIN_SCALE, MANUAL_MAX_SCALE)
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
                        EditTools.ADD_STATES -> detectTapGestures { tapOffset ->
                            clickOffset = Offset(
                                tapOffset.x / (scale * density.density) - offsetX / density.density,
                                tapOffset.y / (scale * density.density) - offsetY / density.density
                            )
                            activeDialog = ActiveDialog.ADD_STATE
                        }
                        EditTools.ADD_TRANSITIONS -> detectTapGestures { tapOffset ->
                            val tapDpX = tapOffset.x / (scale * density.density) - offsetX / density.density
                            val tapDpY = tapOffset.y / (scale * density.density) - offsetY / density.density
                            val positions = viewModel.statePositions
                            val closest = positions.minByOrNull { (_, pos) ->
                                val dx = pos.x + NODE_RADIUS / 2 - tapDpX
                                val dy = pos.y + NODE_RADIUS / 2 - tapDpY
                                sqrt(dx * dx + dy * dy)
                            }
                            closest?.let { (stateIdx, _) ->
                                val state = getStateByIndex(stateIdx)
                                chosenStateForTransitionStart = state
                                chosenStateForTransitionEnd = state
                                activeDialog = ActiveDialog.ADD_TRANSITION
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
                                    modifier = dragModifier,
                                    offsetY = offsetY, offsetX = offsetX,
                                    onTransitionClick = when (editMode) {
                                        EditTools.EDITING -> openTransitionForEditing
                                        EditTools.DELETE -> { transition ->
                                            deleteTransition(transition)
                                            recomposition++
                                            increaseRecomposeValue()
                                        }
                                        else -> null
                                    }
                                )
                                States(
                                    positions = positions,
                                    modifier = dragModifier,
                                    currentEditingState = editMode,
                                    offsetY = offsetY, offsetX = offsetX,
                                    onStateClick = { state ->
                                        when (editMode) {
                                            EditTools.ADD_TRANSITIONS -> {
                                                chosenStateForTransitionStart = state
                                                chosenStateForTransitionEnd = state
                                                activeDialog = ActiveDialog.ADD_TRANSITION
                                            }
                                            EditTools.DELETE -> {
                                                deleteState(state)
                                                viewModel.statePositions.remove(state.index)
                                                recomposition++
                                            }
                                            EditTools.EDITING -> openStateForEditing(state)
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
                        val simFill = when (simulationOutcome) {
                            SimulationOutcome.ACCEPTED -> MaterialTheme.extendedColorScheme.accepted.colorContainer
                            SimulationOutcome.REJECTED -> MaterialTheme.extendedColorScheme.rejected.colorContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                        val simText = when (simulationOutcome) {
                            SimulationOutcome.ACCEPTED -> MaterialTheme.extendedColorScheme.accepted.onColorContainer
                            SimulationOutcome.REJECTED -> MaterialTheme.extendedColorScheme.rejected.onColorContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                        key(recomposeKey) {
                            Transitions(
                                positions = positions,
                                modifier = Modifier, offsetY = offsetY, offsetX = offsetX,
                                onTransitionClick = null
                            )
                        }
                        animationOverlay?.invoke()
                        key(recomposeKey) {
                            States(
                                positions = positions,
                                modifier = Modifier, currentEditingState = null,
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
                text = "${name.ifEmpty { stringResource(R.string.untitled) }}.jff",
                style = MaterialTheme.typography.labelLarge,
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
                text = when (machineType) {
                    MachineType.Finite -> when (isDeterministic()) {
                        true -> stringResource(R.string.dfa)
                        false -> stringResource(R.string.nfa)
                        null -> ""
                    }
                    MachineType.Pushdown -> when (isDeterministic()) {
                        true -> stringResource(R.string.dpda)
                        false -> stringResource(R.string.npda)
                        null -> ""
                    }
                    MachineType.Turing -> when (isDeterministic()) {
                        true -> stringResource(R.string.dtm)
                        false -> stringResource(R.string.ntm)
                        null -> ""
                    }
                },
                style = MaterialTheme.typography.labelLarge,
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
                if (activeDialog == ActiveDialog.ADD_STATE) AddStateWindow(
                    clickOffset, chosenStateForEditing,
                    onPositionAdded = { stateIndex, offset ->
                        viewModel.addStatePosition(stateIndex, offset)
                    }
                ) {
                    activeDialog = null
                    chosenStateForEditing = null
                    recomposition++
                    increaseRecomposeValue()
                }
                val transitionStart = chosenStateForTransitionStart
                val transitionEnd = chosenStateForTransitionEnd
                if (activeDialog == ActiveDialog.ADD_TRANSITION && transitionStart != null && transitionEnd != null) {
                    CreateTransitionWindow(
                        transitionStart,
                        transitionEnd,
                        chosenTransitionName,
                        push,
                        pop
                    ) {
                        activeDialog = null
                        chosenStateForTransitionStart = null
                        chosenStateForTransitionEnd = null
                        chosenTransitionName = null
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
                (this@AutomataView as PushdownMachine).PushdownStack()
            }
        }
    }
}
