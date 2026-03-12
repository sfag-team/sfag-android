package com.sfag.automata.ui.machine

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sfag.R
import com.sfag.automata.domain.common.determinismLabel
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.State
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.ui.AutomataViewModel
import com.sfag.automata.ui.bar.Stack
import com.sfag.automata.ui.bar.Tape
import com.sfag.automata.ui.bar.Toolbar
import com.sfag.automata.ui.common.BAR_HEIGHT
import com.sfag.automata.ui.common.MACHINE_CANVAS_HEIGHT
import com.sfag.automata.ui.common.NODE_RADIUS
import com.sfag.automata.ui.edit.StateDialog
import com.sfag.automata.ui.edit.TransitionDialog
import com.sfag.main.config.MANUAL_MAX_ZOOM
import com.sfag.main.config.MANUAL_MIN_ZOOM
import kotlin.math.sqrt

/** Dialog request shared between MachineView and the bottom panel lists. */
sealed interface DialogRequest {
    data class ForState(
        val tapOffset: Offset,
        val state: State?,
    ) : DialogRequest

    data class ForTransition(
        val fromState: State,
        val toState: State,
        val transitionName: String?,
    ) : DialogRequest
}

/** Unified view for both simulation and editing modes. */
@Composable
fun Machine.MachineView(
    isEditing: Boolean,
    recomposeKey: Int,
    animationOverlay: (@Composable () -> Unit)?,
    dialogRequest: MutableState<DialogRequest?>,
    simulationOutcome: SimulationOutcome?,
    onEdit: () -> Unit,
    onRecompose: () -> Unit,
    onClickName: (() -> Unit)?,
) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val viewModel: AutomataViewModel = hiltViewModel(activity)
    val density = LocalDensity.current

    // Tool state
    var activeTool by remember { mutableStateOf(MachineEditMode.SELECT) }

    // Recomposition trigger
    var localRecomposeKey by remember { mutableIntStateOf(0) }

    val tapeListState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP BAR
        Crossfade(targetState = isEditing, label = "top-bar") { isEditingMode ->
            if (isEditingMode) {
                key(activeTool) {
                    Toolbar(activeTool = activeTool) { newTool -> activeTool = newTool }
                }
            } else {
                key(recomposeKey) {
                    when (this@MachineView) {
                        is FiniteMachine ->
                            Tape(listState = tapeListState, onEdit = onEdit)
                        is PushdownMachine ->
                            Tape(listState = tapeListState, onEdit = onEdit)
                        is TuringMachine ->
                            Tape(listState = tapeListState, onEdit = onEdit)
                    }
                }
            }
        }

        // CENTER: State diagram with zoom/pan - fixed height so bar show/hide never triggers redraw
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(MACHINE_CANVAS_HEIGHT)
                    .padding(1.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            var offsetX by remember { mutableFloatStateOf(viewModel.offsetXCanvas) }
            var offsetY by remember { mutableFloatStateOf(viewModel.offsetYCanvas) }
            var scale by remember { mutableFloatStateOf(viewModel.scaleCanvas) }

            // Center machine in viewport
            val viewW = maxWidth.value
            val viewH = maxHeight.value
            SideEffect {
                if (
                    viewModel.machineAutoCenter &&
                    states.isNotEmpty() &&
                    viewModel.statePositions.isNotEmpty()
                ) {
                    val positions = viewModel.statePositions
                    val minX = positions.values.minOf { it.x }
                    val maxX = positions.values.maxOf { it.x }
                    val minY = positions.values.minOf { it.y }
                    val maxY = positions.values.maxOf { it.y }
                    val centroidX = (minX + maxX + NODE_RADIUS) / 2
                    val centroidY = (minY + maxY + NODE_RADIUS) / 2

                    offsetX = density.density * (viewW / (2f * scale) - centroidX)
                    offsetY = density.density * (viewH / (2f * scale) - centroidY)
                    viewModel.offsetXCanvas = offsetX
                    viewModel.offsetYCanvas = offsetY
                    viewModel.machineAutoCenter = false
                }
            }

            // Bounds for viewport clamping - snapshot state so gesture handler reads latest
            var machineBounds by remember { mutableStateOf<Rect?>(null) }
            SideEffect {
                val positions = viewModel.statePositions
                machineBounds =
                    if (positions.isNotEmpty()) {
                        Rect(
                            left = positions.values.minOf { it.x },
                            top = positions.values.minOf { it.y },
                            right = positions.values.maxOf { it.x },
                            bottom = positions.values.maxOf { it.y },
                        )
                    } else {
                        null
                    }
            }

            val gestureModifier =
                Modifier.pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        scale = (scale * zoom).coerceIn(MANUAL_MIN_ZOOM, MANUAL_MAX_ZOOM)
                        offsetX += centroid.x * (1f / scale - 1f / oldScale) + pan.x / scale
                        offsetY += centroid.y * (1f / scale - 1f / oldScale) + pan.y / scale
                        // Clamp so at least one node stays partially visible
                        val bounds = machineBounds
                        val pxPerDp = density.density
                        if (bounds != null) {
                            offsetX =
                                offsetX.coerceIn(
                                    -(bounds.right + NODE_RADIUS) * pxPerDp,
                                    size.width.toFloat() / scale - bounds.left * pxPerDp,
                                )
                            offsetY =
                                offsetY.coerceIn(
                                    -(bounds.bottom + NODE_RADIUS) * pxPerDp,
                                    size.height.toFloat() / scale - bounds.top * pxPerDp,
                                )
                        }
                        viewModel.offsetXCanvas = offsetX
                        viewModel.offsetYCanvas = offsetY
                        viewModel.scaleCanvas = scale
                    }
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(activeTool, isEditing) {
                            if (!isEditing) return@pointerInput
                            when (activeTool) {
                                MachineEditMode.ADD_STATES ->
                                    detectTapGestures { gestureOffset ->
                                        val tapOffset =
                                            Offset(
                                                gestureOffset.x / (scale * density.density) -
                                                    offsetX / density.density,
                                                gestureOffset.y / (scale * density.density) -
                                                    offsetY / density.density,
                                            )
                                        dialogRequest.value =
                                            DialogRequest.ForState(tapOffset, null)
                                    }
                                MachineEditMode.ADD_TRANSITIONS ->
                                    detectTapGestures { gestureOffset ->
                                        val tapXDp =
                                            gestureOffset.x / (scale * density.density) -
                                                offsetX / density.density
                                        val tapYDp =
                                            gestureOffset.y / (scale * density.density) -
                                                offsetY / density.density
                                        val positions = viewModel.statePositions
                                        val closestStateEntry =
                                            positions.minByOrNull { (_, offset) ->
                                                val dx = offset.x + NODE_RADIUS / 2 - tapXDp
                                                val dy = offset.y + NODE_RADIUS / 2 - tapYDp
                                                sqrt(dx * dx + dy * dy)
                                            }
                                        closestStateEntry?.let { (stateIndex, _) ->
                                            val state = getStateByIndex(stateIndex)
                                            dialogRequest.value =
                                                DialogRequest.ForTransition(state, state, null)
                                        }
                                    }
                                MachineEditMode.SELECT,
                                MachineEditMode.MOVE,
                                MachineEditMode.REMOVE,
                                -> {}
                            }
                        }.then(gestureModifier),
            ) {
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                ) {
                    val positions = viewModel.statePositions

                    if (isEditing) {
                        key(activeTool) {
                            val dragModifier = Modifier

                            key(recomposeKey, localRecomposeKey) {
                                val transitionPaths = computeTransitionPaths(positions, density)
                                TransitionArrows(
                                    transitionPaths = transitionPaths,
                                    modifier = dragModifier,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                    onClickTransition =
                                        when (activeTool) {
                                            MachineEditMode.SELECT -> { transition ->
                                                dialogRequest.value =
                                                    DialogRequest.ForTransition(
                                                        getStateByIndex(transition.fromState),
                                                        getStateByIndex(transition.toState),
                                                        transition.name,
                                                    )
                                            }
                                            MachineEditMode.REMOVE -> { transition ->
                                                val toRemove =
                                                    transitions.filter {
                                                        it.fromState == transition.fromState &&
                                                            it.toState == transition.toState
                                                    }
                                                toRemove.forEach { removeTransition(it) }
                                                localRecomposeKey++
                                                onRecompose()
                                            }
                                            MachineEditMode.MOVE,
                                            MachineEditMode.ADD_STATES,
                                            MachineEditMode.ADD_TRANSITIONS,
                                            -> null
                                        },
                                )
                                StateNodes(
                                    positions = positions,
                                    modifier = dragModifier,
                                    activeTool = activeTool,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                    onClickState = { state ->
                                        when (activeTool) {
                                            MachineEditMode.ADD_TRANSITIONS -> {
                                                dialogRequest.value =
                                                    DialogRequest.ForTransition(
                                                        state,
                                                        state,
                                                        null,
                                                    )
                                            }
                                            MachineEditMode.REMOVE -> {
                                                removeState(state)
                                                viewModel.statePositions.remove(state.index)
                                                localRecomposeKey++
                                            }
                                            MachineEditMode.SELECT -> {
                                                dialogRequest.value =
                                                    DialogRequest.ForState(Offset.Zero, state)
                                            }
                                            MachineEditMode.MOVE,
                                            MachineEditMode.ADD_STATES,
                                            -> {}
                                        }
                                    },
                                    onDragState = { stateIndex, delta ->
                                        viewModel.updateStatePosition(stateIndex, delta)
                                    },
                                    onRecompose = {
                                        localRecomposeKey++
                                        onRecompose()
                                    },
                                )
                            }
                        }
                    } else {
                        val transitionPaths = computeTransitionPaths(positions, density)
                        key(recomposeKey) {
                            TransitionArrows(
                                transitionPaths = transitionPaths,
                                modifier = Modifier,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                onClickTransition = null,
                            )
                        }
                        animationOverlay?.invoke()
                        key(recomposeKey) {
                            StateNodes(
                                positions = positions,
                                modifier = Modifier,
                                activeTool = null,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                simulationOutcome = simulationOutcome,
                                onClickState = {},
                            )
                        }
                    }
                }
            }

            Text(
                text = "${name.ifEmpty { stringResource(R.string.untitled_name) }}.jff",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 8.dp, start = 8.dp)
                        .then(
                            if (onClickName != null) {
                                Modifier.clickable { onClickName() }
                            } else {
                                Modifier
                            },
                        ),
            )

            Text(
                text = determinismLabel() ?: "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
            )

            // Empty state hint
            if (states.isEmpty()) {
                Text(
                    text =
                        stringResource(
                            if (isEditing) R.string.tap_to_add_states else R.string.no_states,
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // Edit dialogs
            if (isEditing) {
                when (val request = dialogRequest.value) {
                    is DialogRequest.ForState ->
                        StateDialog(
                            selectedState = request.state,
                            tapOffset = request.tapOffset,
                            onAddPosition = { stateIndex, offset ->
                                viewModel.addStatePosition(stateIndex, offset)
                            },
                        ) {
                            dialogRequest.value = null
                            localRecomposeKey++
                            onRecompose()
                        }
                    is DialogRequest.ForTransition ->
                        TransitionDialog(request.fromState, request.toState, request.transitionName) {
                            dialogRequest.value = null
                            localRecomposeKey++
                            onRecompose()
                        }
                    null -> {}
                }
            }
        }

        // BOTTOM BAR (PDA stack - slides away in edit mode, canvas height stays stable)
        if (this@MachineView is PushdownMachine) {
            AnimatedVisibility(
                visible = !isEditing,
                exit = shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(BAR_HEIGHT)) {
                    key(recomposeKey) { Stack() }
                }
            }
        }
    }
}
