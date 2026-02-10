package com.sfag.automata.presentation.component.simulation

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.machine.PushDownMachine
import com.sfag.automata.domain.model.machine.TuringMachine
import com.sfag.automata.presentation.component.edit.States
import com.sfag.automata.presentation.component.edit.Transitions

/**
 * Main simulation view that displays the automaton with all states and transitions.
 *
 * Layout:
 * - TOP: Input tape (FA/PDA) or Turing tape (TM)
 * - CENTER: State diagram with transitions (with pinch-to-zoom + drag)
 * - BOTTOM: Stack bar (PDA only)
 */
@SuppressLint("ComposableNaming")
@Composable
fun Machine.SimulateMachine(
    onEditInputClick: () -> Unit
) {
    density = LocalDensity.current
    var offsetX by remember { mutableFloatStateOf(offsetXGraph) }
    var offsetY by remember { mutableFloatStateOf(offsetYGraph) }
    var scale by remember { mutableFloatStateOf(scaleGraph) }

    val gestureModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            val oldScale = scale
            scale = (scale * zoom).coerceIn(0.3f, 3f)
            // offset is in world-space (inside graphicsLayer): screen = (world + offset) * scale
            offsetX += centroid.x * (1f / scale - 1f / oldScale) + pan.x / scale
            offsetY += centroid.y * (1f / scale - 1f / oldScale) + pan.y / scale
            scaleGraph = scale
            offsetXGraph = offsetX
            offsetYGraph = offsetY
        }
    }

    // State diagram with zoom
    Box(
        modifier = gestureModifier
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        ) {
            Transitions(dragModifier = Modifier, offsetY, offsetX, onTransitionClick = null)
            States(dragModifier = Modifier, null, offsetY, offsetX, onStateClick = {})
        }
    }

    // TOP bar: Input tape (FA/PDA) or Turing tape (TM)
    when (machineType) {
        MachineType.Turing -> (this as TuringMachine).TuringTapeBar(onEditClick = onEditInputClick)
        MachineType.Finite, MachineType.Pushdown -> InputTapeBar(onEditClick = onEditInputClick)
    }

    // BOTTOM bar: Stack (PDA only)
    if (machineType == MachineType.Pushdown) {
        (this as PushDownMachine).PushDownStackBar()
    }
}
