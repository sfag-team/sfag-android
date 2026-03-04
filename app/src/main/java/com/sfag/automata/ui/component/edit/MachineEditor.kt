package com.sfag.automata.ui.component.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.machine.EditMachineStates
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.shared.util.Symbols
import com.sfag.automata.ui.component.simulation.EDIT_ICON_SIZE
import com.sfag.automata.ui.component.simulation.TAPE_BAR_HEIGHT
import com.sfag.R

@Composable
fun Machine.EditingMachineBottom(
    recompose: MutableIntState,
    onStateClick: (State) -> Unit,
    onTransitionClick: (Transition) -> Unit,
    onDeleteState: ((State) -> Unit)? = null,
    onDeleteTransition: ((Transition) -> Unit)? = null
) {
    key(recompose.intValue) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.states),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.size(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 160.dp)
                    .padding(bottom = 12.dp)
            ) {
                items(states) { state ->
                    val initialLabel = stringResource(R.string.initial)
                    val finalLabel = stringResource(R.string.final_label)
                    EditListRow(
                        text = state.name +
                            (if (state.initial) "  |  $initialLabel" else "") +
                            (if (state.final) "  |  $finalLabel" else ""),
                        onClick = { onStateClick(state) },
                        onDelete = onDeleteState?.let { { it(state) } }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.transitions),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.size(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 160.dp)
                    .padding(bottom = 12.dp)
            ) {
                items(transitions) { trans ->
                    EditListRow(
                        text = "${getStateByIndex(trans.startState).name} -> ${getStateByIndex(trans.endState).name}  |  " +
                            trans.name.ifEmpty { Symbols.EPSILON } +
                            if (machineType == MachineType.Pushdown) {
                                val pda = trans as PushDownTransition
                                ", ${pda.pop.ifEmpty { Symbols.EPSILON }};${pda.push.ifEmpty { Symbols.EPSILON }}"
                            } else "",
                        onClick = { onTransitionClick(trans) },
                        onDelete = onDeleteTransition?.let { { it(trans) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditListRow(text: String, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onClick() },
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(id = R.drawable.delete),
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
internal fun ToolsRow(
    editMode: EditMachineStates,
    changedMode: (EditMachineStates) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TAPE_BAR_HEIGHT),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeIcon(R.drawable.select, R.string.select, EditMachineStates.EDITING, editMode, changedMode)
        ModeIcon(R.drawable.move, R.string.move, EditMachineStates.MOVE, editMode, changedMode)
        ModeIcon(R.drawable.add_states, R.string.add_states, EditMachineStates.ADD_STATES, editMode, changedMode)
        ModeIcon(R.drawable.add_transitions, R.string.add_transitions, EditMachineStates.ADD_TRANSITIONS, editMode, changedMode)
        ModeIcon(R.drawable.delete, R.string.delete, EditMachineStates.DELETE, editMode, changedMode)
    }
}

@Composable
private fun ModeIcon(
    icon: Int,
    label: Int,
    mode: EditMachineStates,
    currentMode: EditMachineStates,
    changedMode: (EditMachineStates) -> Unit
) {
    val isSelected = currentMode == mode
    Icon(
        painter = painterResource(id = icon),
        contentDescription = stringResource(label),
        modifier = Modifier
            .size(EDIT_ICON_SIZE + 16.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { changedMode(mode) }
            .padding(8.dp),
        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    )
}
