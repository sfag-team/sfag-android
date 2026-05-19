package com.sfag.automata.ui.machine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.State
import com.sfag.automata.domain.machine.Transition
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.automata.domain.simulation.SimConfig
import com.sfag.automata.ui.AutomataViewModel
import com.sfag.automata.ui.bar.Stack
import com.sfag.automata.ui.bar.Tape
import com.sfag.automata.ui.bar.Toolbar
import com.sfag.automata.ui.common.NODE_RADIUS
import com.sfag.automata.ui.common.machineLabel
import com.sfag.automata.ui.edit.StateDialog
import com.sfag.automata.ui.edit.TransitionDialog
import com.sfag.main.config.MAX_ZOOM
import com.sfag.main.config.MIN_ZOOM
import com.sfag.main.config.Symbols

private const val TAP_RADIUS = NODE_RADIUS * 0.5f

internal val cellSize = 48.dp
internal val cellPadding = 4.dp

/** Dialog request shared between MachineEditor and the bottom panel lists. */
sealed interface DialogRequest {
    sealed interface ForState : DialogRequest {
        data class New(val tapOffset: Offset) : ForState

        data class Edit(val state: State) : ForState
    }

    sealed interface ForTransition : DialogRequest {
        data class New(val from: State, val to: State) : ForTransition

        data class Edit(val transition: Transition) : ForTransition
    }
}

/** Unified view for both simulation and editing modes. */
@Composable
fun Machine.MachineEditor(
    editing: Boolean,
    recomposeKey: Int,
    animationOverlay: (@Composable () -> Unit)?,
    dialogRequest: MutableState<DialogRequest?>,
    onEdit: () -> Unit,
    onRecompose: () -> Unit,
) {
    val viewModel: AutomataViewModel = hiltViewModel()
    val density = LocalDensity.current
    val currentFrame = viewModel.currentFrame
    val simStatus = currentFrame?.takeIf { it.isTerminal }?.status
    val selectedConfig = viewModel.selectedConfig
    val activeStateIndices = currentFrame?.activeStateIndices ?: emptySet()

    // Tool state
    var editTool by remember { mutableStateOf(MachineEditMode.SELECT) }

    // Transition paths state for hit testing from the outer pointerInput
    val transitionPathsState = remember { mutableStateOf<List<TransitionPath?>>(emptyList()) }

    val tapeListState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP BAR
        Crossfade(targetState = editing, label = "top-bar") { editingMode ->
            if (editingMode) {
                key(editTool) {
                    Toolbar(currentTool = editTool) { newEditTool -> editTool = newEditTool }
                }
            } else {
                key(recomposeKey) {
                    val symbols =
                        when (selectedConfig) {
                            is SimConfig.Tm -> selectedConfig.tape
                            is SimConfig.Fa,
                            is SimConfig.Pda,
                            null -> fullInput.toList()
                        }
                    val headIndex =
                        when (selectedConfig) {
                            is SimConfig.Fa -> selectedConfig.inputConsumed
                            is SimConfig.Pda -> selectedConfig.inputConsumed
                            is SimConfig.Tm -> selectedConfig.headPosition
                            null -> 0
                        }
                    val muteHead = simStatus != null

                    when (this@MachineEditor) {
                        is FiniteMachine,
                        is PushdownMachine ->
                            Tape(
                                symbols = symbols,
                                headIndex = headIndex,
                                listState = tapeListState,
                                onEdit = onEdit,
                                muteHead = muteHead,
                                muteConsumed = true,
                            )

                        is TuringMachine ->
                            Tape(
                                symbols = symbols,
                                headIndex = headIndex,
                                listState = tapeListState,
                                onEdit = onEdit,
                                blankChar = Symbols.BLANK_CHAR,
                                muteHead = muteHead,
                                infiniteRight = true,
                                infiniteLeft = true,
                            )
                    }
                }
            }
        }

        // CENTER: State diagram with zoom/pan - fixed height so bar show/hide never triggers redraw
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(1.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
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

            Box(
                modifier =
                    Modifier.fillMaxSize()
                        // Canvas pan+zoom (outer = gets events second in Main pass)
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val oldScale = scale
                                scale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                offsetX += centroid.x * (1f / scale - 1f / oldScale) + pan.x / scale
                                offsetY += centroid.y * (1f / scale - 1f / oldScale) + pan.y / scale
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
                        // Tool gestures (inner = gets events first in Main pass)
                        .pointerInput(editTool, editing) {
                            if (!editing) {
                                return@pointerInput
                            }
                            when (editTool) {
                                MachineEditMode.ADD_STATE,
                                MachineEditMode.ADD_TRANSITION,
                                MachineEditMode.SELECT,
                                MachineEditMode.REMOVE ->
                                    awaitEachGesture {
                                        val down = awaitFirstDown()
                                        // Don't consume down - let detectTransformGestures pan/zoom
                                        val touchSlop = viewConfiguration.touchSlop
                                        var tapped = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            // Multi-touch (pinch zoom) - not a tap
                                            if (event.changes.size > 1) {
                                                break
                                            }
                                            val change = event.changes.firstOrNull() ?: break
                                            if (!change.pressed) {
                                                if (
                                                    (change.position - down.position)
                                                        .getDistance() <= touchSlop
                                                ) {
                                                    change.consume()
                                                    tapped = true
                                                }
                                                break
                                            }
                                            if (
                                                (change.position - down.position).getDistance() >
                                                    touchSlop
                                            ) {
                                                break
                                            }
                                        }
                                        if (tapped) {
                                            val gestureOffset = down.position
                                            when (editTool) {
                                                MachineEditMode.ADD_STATE -> {
                                                    val tapOffset =
                                                        Offset(
                                                            gestureOffset.x /
                                                                (scale * density.density) -
                                                                offsetX / density.density,
                                                            gestureOffset.y /
                                                                (scale * density.density) -
                                                                offsetY / density.density,
                                                        )
                                                    dialogRequest.value =
                                                        DialogRequest.ForState.New(tapOffset)
                                                }

                                                MachineEditMode.ADD_TRANSITION -> {
                                                    val tapXDp =
                                                        gestureOffset.x /
                                                            (scale * density.density) -
                                                            offsetX / density.density
                                                    val tapYDp =
                                                        gestureOffset.y /
                                                            (scale * density.density) -
                                                            offsetY / density.density
                                                    val positions = viewModel.statePositions
                                                    val closestStateEntry =
                                                        positions.minByOrNull { (_, offset) ->
                                                            val dx =
                                                                offset.x + NODE_RADIUS / 2 - tapXDp
                                                            val dy =
                                                                offset.y + NODE_RADIUS / 2 - tapYDp
                                                            dx * dx + dy * dy
                                                        }
                                                    closestStateEntry?.let { (stateIndex, _) ->
                                                        val node = getStateByIndex(stateIndex)
                                                        dialogRequest.value =
                                                            DialogRequest.ForTransition.New(
                                                                node,
                                                                node,
                                                            )
                                                    }
                                                }

                                                MachineEditMode.SELECT,
                                                MachineEditMode.REMOVE -> {
                                                    val pxPerDp = density.density
                                                    val tapPxX = gestureOffset.x / scale - offsetX
                                                    val tapPxY = gestureOffset.y / scale - offsetY
                                                    val positions = viewModel.statePositions

                                                    // Check nodes first (circular hitbox)
                                                    val hitNode =
                                                        states.firstOrNull { state ->
                                                            val position =
                                                                positions[state.index]
                                                                    ?: return@firstOrNull false
                                                            val cx =
                                                                (position.x + NODE_RADIUS / 2) *
                                                                    pxPerDp
                                                            val cy =
                                                                (position.y + NODE_RADIUS / 2) *
                                                                    pxPerDp
                                                            val dx = tapPxX - cx
                                                            val dy = tapPxY - cy
                                                            dx * dx + dy * dy <
                                                                NODE_RADIUS * NODE_RADIUS
                                                        }

                                                    if (hitNode != null) {
                                                        if (editTool == MachineEditMode.SELECT) {
                                                            dialogRequest.value =
                                                                DialogRequest.ForState.Edit(hitNode)
                                                        } else if (
                                                            editTool == MachineEditMode.REMOVE
                                                        ) {
                                                            removeState(hitNode)
                                                            viewModel.statePositions.remove(
                                                                hitNode.index
                                                            )
                                                            viewModel.markDirty()
                                                            onRecompose()
                                                        }
                                                        return@awaitEachGesture
                                                    }

                                                    // Check transitions (path hitbox)
                                                    val paths = transitionPathsState.value
                                                    for ((index, path) in paths.withIndex()) {
                                                        if (index >= transitions.size) {
                                                            break
                                                        }
                                                        if (
                                                            path != null &&
                                                                isPathHit(
                                                                    path.arrowBody,
                                                                    tapPxX,
                                                                    tapPxY,
                                                                    TAP_RADIUS,
                                                                )
                                                        ) {
                                                            val transition = transitions[index]
                                                            if (
                                                                editTool == MachineEditMode.SELECT
                                                            ) {
                                                                dialogRequest.value =
                                                                    DialogRequest.ForTransition
                                                                        .Edit(transition)
                                                            } else if (
                                                                editTool == MachineEditMode.REMOVE
                                                            ) {
                                                                transitions
                                                                    .filter {
                                                                        it.fromState ==
                                                                            transition.fromState &&
                                                                            it.toState ==
                                                                                transition.toState
                                                                    }
                                                                    .forEach {
                                                                        removeTransition(it)
                                                                    }
                                                                viewModel.markDirty()
                                                                onRecompose()
                                                            }
                                                            break
                                                        }
                                                    }
                                                }

                                                MachineEditMode.MOVE -> {}
                                            }
                                        }
                                    }

                                MachineEditMode.MOVE ->
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val pxPerDp = density.density
                                        val tapPxX = down.position.x / scale - offsetX
                                        val tapPxY = down.position.y / scale - offsetY
                                        val positions = viewModel.statePositions
                                        val hitIndex =
                                            states
                                                .firstOrNull { state ->
                                                    val position =
                                                        positions[state.index]
                                                            ?: return@firstOrNull false
                                                    val cx =
                                                        (position.x + NODE_RADIUS / 2) * pxPerDp
                                                    val cy =
                                                        (position.y + NODE_RADIUS / 2) * pxPerDp
                                                    val dx = tapPxX - cx
                                                    val dy = tapPxY - cy
                                                    dx * dx + dy * dy < NODE_RADIUS * NODE_RADIUS
                                                }
                                                ?.index

                                        if (hitIndex != null) {
                                            down.consume()
                                            do {
                                                val event = awaitPointerEvent()
                                                event.changes.forEach { change ->
                                                    val delta =
                                                        change.position - change.previousPosition
                                                    change.consume()
                                                    viewModel.updateStatePosition(
                                                        hitIndex,
                                                        Offset(
                                                            delta.x / (scale * pxPerDp),
                                                            delta.y / (scale * pxPerDp),
                                                        ),
                                                    )
                                                }
                                            } while (event.changes.any { it.pressed })
                                        }
                                        // No node hit - fall through, detectTransformGestures
                                        // handles pan/zoom
                                    }
                            }
                        }
            ) {
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                ) {
                    val positions = viewModel.statePositions

                    if (editing) {
                        key(editTool) {
                            key(recomposeKey) {
                                val transitionPaths =
                                    computeTransitionPaths(positions, density.density)
                                SideEffect { transitionPathsState.value = transitionPaths }
                                TransitionArrows(
                                    transitionPaths = transitionPaths,
                                    modifier = Modifier,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                )
                                StateNodes(
                                    positions = positions,
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                    editing = true,
                                    activeStateIndices = activeStateIndices,
                                )
                            }
                        }
                    } else {
                        val transitionPaths = computeTransitionPaths(positions, density.density)
                        key(recomposeKey) {
                            TransitionArrows(
                                transitionPaths = transitionPaths,
                                modifier = Modifier,
                                offsetX = offsetX,
                                offsetY = offsetY,
                            )
                        }
                        animationOverlay?.invoke()
                        key(recomposeKey) {
                            StateNodes(
                                positions = positions,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                activeStateIndices = activeStateIndices,
                                simStatus = simStatus,
                            )
                        }
                    }
                }
            }

            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 8.dp),
            )

            Text(
                text = machineLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
            )

            // Empty state hint
            if (states.isEmpty()) {
                Text(
                    text =
                        stringResource(
                            if (editing) R.string.tap_to_add_states else R.string.no_states
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // Edit dialogs
            if (editing) {
                when (val request = dialogRequest.value) {
                    is DialogRequest.ForState ->
                        StateDialog(
                            request = request,
                            onAddPosition = { stateIndex, offset ->
                                viewModel.addStatePosition(stateIndex, offset)
                            },
                            onDismiss = {
                                dialogRequest.value = null
                                onRecompose()
                            },
                            onConfirm = {
                                viewModel.markDirty()
                                dialogRequest.value = null
                                onRecompose()
                            },
                        )

                    is DialogRequest.ForTransition ->
                        TransitionDialog(
                            request = request,
                            onDismiss = {
                                dialogRequest.value = null
                                onRecompose()
                            },
                            onConfirm = {
                                viewModel.markDirty()
                                dialogRequest.value = null
                                onRecompose()
                            },
                        )

                    null -> {}
                }
            }
        }

        // BOTTOM BAR (PDA stack - slides away in edit mode, canvas height stays stable)
        if (this@MachineEditor is PushdownMachine) {
            AnimatedVisibility(
                visible = !editing,
                exit = shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(cellSize + cellPadding * 2)) {
                    key(recomposeKey) {
                        val pdaConfig = selectedConfig as? SimConfig.Pda
                        Stack(symbols = pdaConfig?.stack ?: listOf(Symbols.INITIAL_STACK_SYMBOL))
                    }
                }
            }
        }
    }
}
