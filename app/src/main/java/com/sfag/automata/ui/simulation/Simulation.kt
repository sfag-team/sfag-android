package com.sfag.automata.ui.simulation

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
import com.sfag.automata.core.machine.BottomPushDownBar
import com.sfag.automata.core.machine.Machine
import com.sfag.automata.core.machine.MachineType
import com.sfag.automata.core.machine.PushDownMachine
import com.sfag.automata.ui.edit.InputBar
import com.sfag.automata.ui.edit.States
import com.sfag.automata.ui.edit.Transitions


/**
 * draws machine with all states and transitions
 */
@SuppressLint("ComposableNaming", "SuspiciousIndentation")
@Composable
fun Machine.SimulateMachine() {
    context = LocalContext.current
    density = LocalDensity.current
    var offsetX by remember {
        mutableFloatStateOf(offsetXGraph)
    }
    var offsetY by remember {
        mutableFloatStateOf(offsetYGraph)
    }

    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            offsetX += dragAmount.x
            offsetY += dragAmount.y
            offsetXGraph = offsetX
            offsetYGraph = offsetY
        }
    }

    Transitions(dragModifier = dragModifier, offsetY, offsetX, onTransitionClick = null)
    States(dragModifier = dragModifier, null, offsetY, offsetX, onStateClick = {})
    InputBar()
    if (machineType == MachineType.Pushdown) {
        BottomPushDownBar(this as PushDownMachine)
    }
}
