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
import com.sfag.automata.domain.common.determinismLabel
import com.sfag.automata.domain.machine.Config
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.State
import com.sfag.automata.ui.AutomataViewModel
import com.sfag.automata.ui.bar.Stack
import com.sfag.automata.ui.bar.Tape
import com.sfag.automata.ui.bar.Toolbar
import com.sfag.automata.ui.common.NODE_RADIUS
import com.sfag.automata.ui.edit.StateDialog
import com.sfag.automata.ui.edit.TransitionDialog
import com.sfag.main.config.MAX_ZOOM
import com.sfag.main.config.MIN_ZOOM

private const val TAP_RADIUS = NODE_RADIUS * 0.5f

internal val cellSize = 48.dp
internal val cellPadding = 4.dp

/** Dialog request shared between MachineEditor and the bottom panel lists. */
sealed interface DialogRequest {
    data class ForState(val tapOffset: Offset, val state: State?) : DialogRequest

    data class ForTransition(
        val fromState: State,
        val toState: State,
        val transitionName: String?,
    ) : DialogRequest
}

/** Unified view for both simulation and editing modes. */
@Composable
fun Machine.MachineEditor(
    isEditing: Boolean,
    recomposeKey: Int,
    animationOverlay: (@Composable () -> Unit)?,
    dialogRequest: MutableState<DialogRequest?>,
    onEdit: () -> Unit,
    onRecompose: () -> Unit,
) {
    val viewModel: AutomataViewModel = hiltViewModel()
    val density = LocalDensity.current
    val currentFrame = viewModel.currentFrame
    val simulationOutcome = currentFrame?.takeIf { it.isTerminal }?.outcome
    val displayConfig = viewModel.displayConfig
    val activeStateIndices = currentFrame?.activeStateIndices ?: emptySet()

    // Tool state
    var activeTool by remember { mutableStateOf(MachineEditMode.SELECT) }

    // Transition paths state for hit testing from the outer pointerInput
    val transitionPathsState = remember { mutableStateOf<List<TransitionPath?>>(emptyList()) }

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
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val headColor =
                        if (simulationOutcome != null) primaryColor.copy(alpha = 0.38f)
                        else primaryColor
                    when (displayConfig) {
                        is Config.Tm ->
                            Tape(
                                symbols = displayConfig.tape,
                                headIndex = displayConfig.headPosition,
                                listState = tapeListState,
                                headColor = headColor,
                                onEdit = onEdit,
                                infiniteRight = true,
                                infiniteLeft = true,
                            )

                        is Config.Fa,
                        is Config.Pda,
                        null -> {
                            val symbols = fullInput.toList()
                            val consumed =
                                when (displayConfig) {
                                    is Config.Fa -> displayConfig.inputConsumed
                                    is Config.Pda -> displayConfig.inputConsumed
                                    null,
                                    is Config.Tm -> 0
                                }
                            Tape(
                                symbols = symbols,
                                headIndex = consumed.coerceIn(0, symbols.size),
                                listState = tapeListState,
                                headColor = headColor,
                                isConsumedShown = true,
                                onEdit = onEdit,
                                infiniteRight = false,
                            )
                        }
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
                        .pointerInput(activeTool, isEditing) {
                            if (!isEditing) {
                                return@pointerInput
                            }
                            when (activeTool) {
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
                                            when (activeTool) {
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
                                                        DialogRequest.ForState(tapOffset, null)
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
                                                        val state = getStateByIndex(stateIndex)
                                                        dialogRequest.value =
                                                            DialogRequest.ForTransition(
                                                                state,
                                                                state,
                                                                null,
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
                                                    val hitState =
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

                                                    if (hitState != null) {
                                                        if (activeTool == MachineEditMode.SELECT) {
                                                            dialogRequest.value =
                                                                DialogRequest.ForState(
                                                                    Offset.Zero,
                                                                    hitState,
                                                                )
                                                        } else if (
                                                            activeTool == MachineEditMode.REMOVE
                                                        ) {
                                                            removeState(hitState)
                                                            viewModel.statePositions.remove(
                                                                hitState.index
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
                                                                activeTool == MachineEditMode.SELECT
                                                            ) {
                                                                dialogRequest.value =
                                                                    DialogRequest.ForTransition(
                                                                        getStateByIndex(
                                                                            transition.fromState
                                                                        ),
                                                                        getStateByIndex(
                                                                            transition.toState
                                                                        ),
                                                                        transition.read,
                                                                    )
                                                            } else if (
                                                                activeTool == MachineEditMode.REMOVE
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

                    if (isEditing) {
                        key(activeTool) {
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
                                    isEditing = true,
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
                                simulationOutcome = simulationOutcome,
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
                            if (isEditing) R.string.tap_to_add_states else R.string.no_states
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
                            request.fromState,
                            request.toState,
                            request.transitionName,
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
                visible = !isEditing,
                exit = shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(cellSize + cellPadding * 2)) {
                    key(recomposeKey) {
                        val pdaConfig = displayConfig as? Config.Pda
                        Stack(symbols = pdaConfig?.stack ?: emptyList())
                    }
                }
            }
        }
    }
}
