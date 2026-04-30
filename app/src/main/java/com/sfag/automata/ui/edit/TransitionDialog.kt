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
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.State
import com.sfag.automata.domain.machine.TapeDirection
import com.sfag.automata.domain.machine.Transition
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.main.config.Symbols.BLANK_CHAR
import com.sfag.main.data.JffUtils
import com.sfag.main.ui.component.CancelButton
import com.sfag.main.ui.component.ConfirmButton
import com.sfag.main.ui.component.DefaultDialog
import com.sfag.main.ui.component.DefaultTextField
import com.sfag.main.ui.component.DropdownSelector

/** Dialog for creating or editing a transition. */
@Composable
internal fun Machine.TransitionDialog(
    from: State,
    to: State,
    readSymbol: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    // PDA-specific state
    val pdaTransition =
        if (this@TransitionDialog is PushdownMachine && readSymbol != null) {
            this@TransitionDialog.pdaTransitions.firstOrNull {
                it.fromState == from.index && it.toState == to.index && it.read == readSymbol
            }
        } else {
            null
        }

    // TM-specific state
    val tmTransition =
        if (this@TransitionDialog is TuringMachine && readSymbol != null) {
            this@TransitionDialog.tmTransitions.firstOrNull {
                it.fromState == from.index && it.toState == to.index && it.read == readSymbol
            }
        } else {
            null
        }

    var fromState: State by remember { mutableStateOf(from) }
    var toState: State by remember { mutableStateOf(to) }
    var newRead by remember { mutableStateOf(readSymbol ?: "") }
    var popSymbol by remember { mutableStateOf(pdaTransition?.pop ?: "") }
    var pushSymbol by remember { mutableStateOf(pdaTransition?.push ?: "") }
    var writeSymbol by remember { mutableStateOf(tmTransition?.write?.toString() ?: "") }
    var tapeDirection by remember { mutableStateOf(tmTransition?.direction ?: TapeDirection.RIGHT) }

    DefaultDialog(
        onDismissRequest = onDismiss,
        buttons = {
            CancelButton(onClick = onDismiss)
            ConfirmButton(
                onClick = {
                    when (val machine = this@TransitionDialog) {
                        is FiniteMachine -> {
                            val normalizedRead = JffUtils.normalizeEpsilon(newRead.trim())
                            if (readSymbol == null) {
                                machine.addNewTransition(fromState, toState, normalizedRead)
                            } else {
                                machine.transitions
                                    .firstOrNull(matchTransition(from, to, readSymbol))
                                    ?.let {
                                        it.fromState = fromState.index
                                        it.toState = toState.index
                                        it.read = normalizedRead
                                    }
                            }
                        }
                        is PushdownMachine -> {
                            val normalizedRead = JffUtils.normalizeEpsilon(newRead.trim())
                            val normalizedPop = JffUtils.normalizeEpsilon(popSymbol.trim())
                            val normalizedPush = JffUtils.normalizeEpsilon(pushSymbol.trim())
                            if (readSymbol == null) {
                                machine.addNewTransition(
                                    fromState,
                                    toState,
                                    normalizedRead,
                                    normalizedPop,
                                    normalizedPush,
                                )
                            } else {
                                machine.pdaTransitions
                                    .firstOrNull(matchTransition(from, to, readSymbol))
                                    ?.let {
                                        it.fromState = fromState.index
                                        it.toState = toState.index
                                        it.read = normalizedRead
                                        it.pop = normalizedPop
                                        it.push = normalizedPush
                                    }
                            }
                        }
                        is TuringMachine -> {
                            val tmRead =
                                newRead.trim().firstOrNull()?.toString() ?: BLANK_CHAR.toString()
                            val tmWrite = writeSymbol.trim().firstOrNull() ?: BLANK_CHAR
                            if (readSymbol == null) {
                                machine.addNewTransition(
                                    fromState,
                                    toState,
                                    tmRead,
                                    tmWrite,
                                    tapeDirection,
                                )
                            } else {
                                machine.tmTransitions
                                    .firstOrNull(matchTransition(from, to, readSymbol))
                                    ?.let {
                                        it.fromState = fromState.index
                                        it.toState = toState.index
                                        it.direction = tapeDirection
                                        it.read = tmRead
                                        it.write = tmWrite
                                    }
                            }
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
                defaultSelectedIndex = states.indexOf(from),
                onSelectItem = { if (it != fromState) fromState = it },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.to_state),
                style = MaterialTheme.typography.bodyMedium,
            )
            DropdownSelector(
                items = states,
                defaultSelectedIndex = states.indexOf(to),
                onSelectItem = { if (it != toState) toState = it },
                modifier = Modifier.weight(1f),
            )
        }

        // Transition read field
        DefaultTextField(
            value = newRead,
            onValueChange = { newRead = it },
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
                defaultSelectedIndex = TapeDirection.entries.indexOf(tapeDirection),
                onSelectItem = { tapeDirection = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.transition_direction),
            )
        }
    }
}

/** Predicate identifying the transition being edited by its (from, to, read) tuple. */
private fun <T : Transition> matchTransition(
    from: State,
    to: State,
    readSymbol: String,
): (T) -> Boolean = {
    it.fromState == from.index && it.toState == to.index && it.read == readSymbol
}
