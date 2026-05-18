package com.sfag.automata.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.automata.domain.machine.FaTransition
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PdaTransition
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.State
import com.sfag.automata.domain.machine.TapeDirection
import com.sfag.automata.domain.machine.TmTransition
import com.sfag.automata.domain.machine.Transition
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.automata.ui.machine.DialogRequest
import com.sfag.main.data.JffUtils
import com.sfag.main.ui.component.CancelButton
import com.sfag.main.ui.component.ConfirmButton
import com.sfag.main.ui.component.DefaultDialog
import com.sfag.main.ui.component.DefaultTextField
import com.sfag.main.ui.component.DropdownSelector

/** Dialog for creating or editing a transition. */
@Composable
internal fun Machine.TransitionDialog(
    request: DialogRequest.ForTransition,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val transition: Transition? =
        when (request) {
            is DialogRequest.ForTransition.Edit -> request.transition
            is DialogRequest.ForTransition.New -> null
        }
    val pda = transition as? PdaTransition
    val tm = transition as? TmTransition

    var from: State by
        remember(request) {
            mutableStateOf(
                when (request) {
                    is DialogRequest.ForTransition.New -> request.from
                    is DialogRequest.ForTransition.Edit ->
                        getStateByIndex(request.transition.fromState)
                }
            )
        }
    var to: State by
        remember(request) {
            mutableStateOf(
                when (request) {
                    is DialogRequest.ForTransition.New -> request.to
                    is DialogRequest.ForTransition.Edit ->
                        getStateByIndex(request.transition.toState)
                }
            )
        }
    var read by remember { mutableStateOf(transition?.read ?: "") }
    var popSymbol by remember { mutableStateOf(pda?.pop ?: "") }
    var pushSymbol by remember { mutableStateOf(pda?.push ?: "") }
    var writeSymbol by remember { mutableStateOf(tm?.write?.toString() ?: "") }
    var tapeDirection by remember { mutableStateOf(tm?.direction ?: TapeDirection.RIGHT) }

    DefaultDialog(
        onDismissRequest = onDismiss,
        buttons = {
            CancelButton(onClick = onDismiss)
            ConfirmButton(
                onClick = {
                    when (val machine = this@TransitionDialog) {
                        is FiniteMachine -> {
                            val transition = transition as? FaTransition
                            val normalizedRead = JffUtils.normalizeEpsilon(read.trim())
                            val newTransition = FaTransition(from.index, to.index, normalizedRead)
                            replaceOrAddTransition(
                                list = machine.faTransitions,
                                transition = transition,
                                newTransition = newTransition,
                            )
                        }
                        is PushdownMachine -> {
                            val transition = transition as? PdaTransition
                            val normalizedRead = JffUtils.normalizeEpsilon(read.trim())
                            val normalizedPop = JffUtils.normalizeEpsilon(popSymbol.trim())
                            val normalizedPush = JffUtils.normalizeEpsilon(pushSymbol.trim())
                            val newTransition =
                                PdaTransition(
                                    from.index,
                                    to.index,
                                    normalizedRead,
                                    normalizedPop,
                                    normalizedPush,
                                )
                            replaceOrAddTransition(
                                list = machine.pdaTransitions,
                                transition = transition,
                                newTransition = newTransition,
                            )
                        }
                        is TuringMachine -> {
                            val transition = transition as? TmTransition
                            val tmRead = JffUtils.normalizeBlank(read.take(1))
                            val tmWrite = JffUtils.normalizeBlank(writeSymbol.take(1)).first()
                            val newTransition =
                                TmTransition(from.index, to.index, tmRead, tmWrite, tapeDirection)
                            replaceOrAddTransition(
                                list = machine.tmTransitions,
                                transition = transition,
                                newTransition = newTransition,
                            )
                        }
                    }
                    onConfirm()
                }
            )
        },
    ) {
        // From/To dropdowns row
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.from_state),
                style = MaterialTheme.typography.bodyMedium,
            )
            DropdownSelector(
                items = states,
                selectedIndex = states.indexOf(from),
                onSelectItem = { from = it },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.to_state),
                style = MaterialTheme.typography.bodyMedium,
            )
            DropdownSelector(
                items = states,
                selectedIndex = states.indexOf(to),
                onSelectItem = { to = it },
                modifier = Modifier.weight(1f),
            )
        }

        // Transition read field
        DefaultTextField(
            value = read,
            onValueChange = { read = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.transition_read),
        )

        // Pop and Push fields (PDA only)
        if (this@TransitionDialog is PushdownMachine) {
            DefaultTextField(
                value = popSymbol,
                onValueChange = { popSymbol = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.stack_pop),
            )
            DefaultTextField(
                value = pushSymbol,
                onValueChange = { pushSymbol = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.stack_push),
            )
        }

        // Write and Direction fields (TM only)
        if (this@TransitionDialog is TuringMachine) {
            DefaultTextField(
                value = writeSymbol,
                onValueChange = { writeSymbol = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.transition_write),
            )
            DropdownSelector(
                items = TapeDirection.entries,
                selectedIndex = TapeDirection.entries.indexOf(tapeDirection),
                onSelectItem = { tapeDirection = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.transition_direction),
            )
        }
    }
}

/**
 * Replaces [transition] in [list] with [newTransition], or appends [newTransition] when
 * [transition] is null. If [newTransition] equals another existing element (data class equality),
 * [transition] is removed instead so the list stays free of duplicates.
 */
private fun <T : Transition> replaceOrAddTransition(
    list: MutableList<T>,
    transition: T?,
    newTransition: T,
) {
    if (transition == null) {
        if (newTransition !in list) {
            list.add(newTransition)
        }
        return
    }
    val index = list.indexOfFirst { it === transition }
    if (index < 0) {
        return
    }
    val collidesWithOther = list.withIndex().any { (i, t) -> i != index && t == newTransition }
    if (collidesWithOther) {
        list.removeAt(index)
    } else {
        list[index] = newTransition
    }
}
