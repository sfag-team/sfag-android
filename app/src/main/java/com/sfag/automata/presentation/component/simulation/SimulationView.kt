package com.sfag.automata.presentation.component.simulation

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
 * - CENTER: State diagram with transitions
 * - BOTTOM: Stack bar (PDA only)
 */
@SuppressLint("ComposableNaming", "SuspiciousIndentation")
@Composable
fun Machine.SimulateMachine(
    onEditInputClick: () -> Unit
) {
    context = LocalContext.current
    density = LocalDensity.current
    var offsetX by remember { mutableFloatStateOf(offsetXGraph) }
    var offsetY by remember { mutableFloatStateOf(offsetYGraph) }

    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            offsetX += dragAmount.x
            offsetY += dragAmount.y
            offsetXGraph = offsetX
            offsetYGraph = offsetY
        }
    }

    // State diagram
    Transitions(dragModifier = dragModifier, offsetY, offsetX, onTransitionClick = null)
    States(dragModifier = dragModifier, null, offsetY, offsetX, onStateClick = {})

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
