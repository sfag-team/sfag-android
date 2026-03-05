package com.sfag.automata.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.sfag.R
import com.sfag.automata.TAPE_BAR_HEIGHT
import com.sfag.automata.TAPE_CELL_PADDING
import com.sfag.automata.TAPE_CELL_SIZE
import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.machine.MachineType
import com.sfag.automata.model.machine.State
import com.sfag.automata.model.transition.PushdownTransition
import com.sfag.automata.model.transition.Transition
import com.sfag.shared.Symbols

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
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.states),
                style = MaterialTheme.typography.titleLarge,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                items(states) { state ->
                    val initialLabel = stringResource(R.string.initial_label)
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.transitions),
                style = MaterialTheme.typography.titleLarge,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                items(transitions) { trans ->
                    EditListRow(
                        text = "${getStateByIndex(trans.startState).name} -> ${getStateByIndex(trans.endState).name}  |  " +
                            trans.name.ifEmpty { "${Symbols.EPSILON}" } +
                            if (machineType == MachineType.Pushdown) {
                                val pda = trans as PushdownTransition
                                ", ${pda.pop.ifEmpty { "${Symbols.EPSILON}" }};${pda.push.ifEmpty { "${Symbols.EPSILON}" }}"
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
    editMode: EditTools,
    changedMode: (EditTools) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TAPE_BAR_HEIGHT),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = CenterVertically
    ) {
        ModeIcon(R.drawable.select, R.string.select, EditTools.EDITING, editMode, changedMode)
        ModeIcon(R.drawable.move, R.string.move, EditTools.MOVE, editMode, changedMode)
        ModeIcon(R.drawable.add_states, R.string.add_states, EditTools.ADD_STATES, editMode, changedMode)
        ModeIcon(R.drawable.add_transitions, R.string.add_transitions, EditTools.ADD_TRANSITIONS, editMode, changedMode)
        ModeIcon(R.drawable.delete, R.string.delete, EditTools.DELETE, editMode, changedMode)
    }
}

@Composable
private fun ModeIcon(
    icon: Int,
    label: Int,
    mode: EditTools,
    currentMode: EditTools,
    changedMode: (EditTools) -> Unit
) {
    val isSelected = currentMode == mode
    Icon(
        painter = painterResource(id = icon),
        contentDescription = stringResource(label),
        modifier = Modifier
            .size(TAPE_CELL_SIZE)
            .clip(MaterialTheme.shapes.small)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { changedMode(mode) }
            .padding(TAPE_CELL_PADDING),
        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    )
}
