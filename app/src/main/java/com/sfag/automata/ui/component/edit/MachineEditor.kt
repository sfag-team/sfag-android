package com.sfag.automata.ui.component.edit

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.shared.theme.perlamutr_white
import com.sfag.automata.presentation.model.EditMachineStates
import com.sfag.R


@SuppressLint("DefaultLocale", "UnrememberedMutableState")
@Composable
fun Machine.EditingMachine( increaseRecomposeValue :() -> Unit) {
    var recomposition by remember {
        mutableIntStateOf(0)
    }
    var offsetX by remember {
        mutableFloatStateOf(offsetXGraph)
    }
    var offsetY by remember {
        mutableFloatStateOf(offsetYGraph)
    }
    var clickOffset by remember {
        mutableStateOf(Offset(0f, 0f))
    }
    var currentEditingState by remember {
        mutableStateOf(editMode)
    }
    var addStateWindowFocused by remember {
        mutableStateOf(false)
    }
    var addTransitionWindowFocused by remember {
        mutableStateOf(false)
    }
    var choosedStateForTransitionStart by remember {
        mutableStateOf<State?>(null)
    }

    var choosedStateForTransitionEnd by remember {
        mutableStateOf<State?>(null)
    }

    var choosedTransitionName by remember {
        mutableStateOf<String?>(null)
    }

    var push by remember {
        mutableStateOf<String?>(null)
    }

    var pop by remember {
        mutableStateOf<String?>(null)
    }

    var chosedStateForEditing by remember {
        mutableStateOf<State?>(null)
    }

    LaunchedEffect(Unit) {
        onBottomStateClicked = { state ->
            chosedStateForEditing = state
            addStateWindowFocused = true
        }

        onBottomTransitionClicked = { transition ->
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
    }


    key(currentEditingState) {
        val dragModifier = when (currentEditingState) {

            EditMachineStates.ADD_STATES -> {
                Modifier.pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val localOffset = getDpOffsetWithPxOffset(
                            Offset(offset.x, offset.y)
                        ) ?: return@detectTapGestures
                        clickOffset = localOffset
                        addStateWindowFocused = true
                    }
                }
            }

            else -> Modifier
        }

        key(recomposition) {
            Transitions(
                dragModifier = dragModifier,
                offsetY,
                offsetX,
                onTransitionClick = when (currentEditingState) {
                    EditMachineStates.EDITING -> { transition ->
                        choosedStateForTransitionStart = getStateByIndex(transition.startState)
                        choosedStateForTransitionEnd = getStateByIndex(transition.endState)
                        choosedTransitionName = transition.name
                        if (machineType == MachineType.Pushdown) {
                            transition as PushDownTransition
                            push = transition.push
                            pop = transition.pop
                        }
                        addTransitionWindowFocused = true
                    }

                    EditMachineStates.DELETE -> { transition ->
                        deleteTransition(transition)
                        recomposition++
                        increaseRecomposeValue()
                    }

                    else -> null
                }
            )
            States(dragModifier = dragModifier,
                currentEditingState,
                offsetY,
                offsetX,
                onStateClick = { state ->
                    when (currentEditingState) {
                        EditMachineStates.ADD_TRANSITIONS -> {
                            choosedStateForTransitionStart = state
                            choosedStateForTransitionEnd = state
                            addTransitionWindowFocused = true
                        }

                        EditMachineStates.DELETE -> {
                            deleteState(state)
                            recomposition++
                        }

                        EditMachineStates.EDITING -> {
                            chosedStateForEditing = state
                            addStateWindowFocused = true
                        }

                        else -> {}
                    }

                },
                recompose = {
                    recomposition++
                    increaseRecomposeValue()
                }
            )
        }

        ToolsRow {
            currentEditingState = editMode
        }
        if (addStateWindowFocused) AddStateWindow(clickOffset, chosedStateForEditing) {
            addStateWindowFocused = false
            chosedStateForEditing = null
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
                increaseRecomposeValue()
            }
        }
    }
}


@Composable
fun Machine.EditingMachineBottom(recompose: MutableIntState) {
    key(recompose.intValue){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .clip(MaterialTheme.shapes.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.size(8.dp))
            Text("States", fontSize = 30.sp)
            Spacer(modifier = Modifier.size(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(190.dp)
                    .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(MaterialTheme.shapes.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(states) { state ->

                    Spacer(modifier = Modifier.size(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.medium
                            )
                            .clickable {
                                onBottomStateClicked(state)
                            },
                        verticalAlignment = CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(text = "(${state.index}) ${state.name}:${if(state.finite) " final" else ""}${if(state.initial) " initial" else ""}", fontSize = 24.sp)
                    }

                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text("Transitions", fontSize = 30.sp)
            Spacer(modifier = Modifier.size(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(190.dp)
                    .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(MaterialTheme.shapes.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(transitions) { trans ->
                    Spacer(modifier = Modifier.size(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.medium
                            )
                            .clickable {
                                onBottomTransitionClicked(trans)
                            },
                        verticalAlignment = CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = "${getStateByIndex(trans.startState).name} -> ${
                                getStateByIndex(
                                    trans.endState
                                ).name
                            }: \"${trans.name}\" ${if(machineType==MachineType.Pushdown) "; "+(trans as PushDownTransition).pop+"; "+ trans.push+"." else "."}", fontSize = 24.sp
                        )
                    }

                }
            }
        }
    }
}

@Composable
internal fun Machine.ToolsRow(changedMode: (EditMachineStates) -> Unit) {
    val spaceSize = 20.dp
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.13f)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(perlamutr_white)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(13f)
                .background(perlamutr_white),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.edit_icon),
                contentDescription = stringResource(R.string.edit_icon),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(if (editMode == EditMachineStates.EDITING) MaterialTheme.colorScheme.primaryContainer else Color.White)
                    .clickable {
                        editMode = EditMachineStates.EDITING
                        changedMode(editMode)
                    }
            )
            Spacer(modifier = Modifier.width(spaceSize))
            Icon(
                painter = painterResource(id = R.drawable.add_states),
                contentDescription = stringResource(
                    R.string.add_states_icon
                ),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(if (editMode == EditMachineStates.ADD_STATES) MaterialTheme.colorScheme.primaryContainer else Color.White)
                    .clickable {
                        editMode = EditMachineStates.ADD_STATES
                        changedMode(editMode)
                    }
            )
            Spacer(modifier = Modifier.width(spaceSize))
            Icon(
                painter = painterResource(id = R.drawable.add_transitions),
                contentDescription = stringResource(
                    R.string.add_transitions_icon
                ),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(if (editMode == EditMachineStates.ADD_TRANSITIONS) MaterialTheme.colorScheme.primaryContainer else Color.White)
                    .clickable {
                        editMode = EditMachineStates.ADD_TRANSITIONS
                        changedMode(editMode)
                    }
            )
            Spacer(modifier = Modifier.width(spaceSize))
            Icon(
                painter = painterResource(id = R.drawable.bin),
                contentDescription = stringResource(
                    R.string.bin_icon
                ),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(if (editMode == EditMachineStates.DELETE) MaterialTheme.colorScheme.primaryContainer else Color.White)
                    .clickable {
                        editMode = EditMachineStates.DELETE
                        changedMode(editMode)
                    }
            )
            Spacer(modifier = Modifier.width(spaceSize))
            Icon(
                painter = painterResource(id = R.drawable.move_icon),
                contentDescription = stringResource(
                    R.string.move_icon
                ),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(if (editMode == EditMachineStates.MOVE) MaterialTheme.colorScheme.primaryContainer else Color.White)
                    .clickable {
                        editMode = EditMachineStates.MOVE
                        changedMode(editMode)
                    }
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(perlamutr_white)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
