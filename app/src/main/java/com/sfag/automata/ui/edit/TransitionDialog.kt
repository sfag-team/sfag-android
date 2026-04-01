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
import com.sfag.main.ui.component.DefaultDialog
import com.sfag.main.ui.component.DefaultTextField
import com.sfag.main.ui.component.DropdownSelector

/** Dialog for creating or editing a transition. */
@Composable
internal fun Machine.TransitionDialog(
    from: State,
    to: State,
    existingTransitionName: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    // PDA-specific state - only initialized for PushdownMachine
    val existingPdaTransition =
        if (this@TransitionDialog is PushdownMachine && existingTransitionName != null) {
            pdaTransitions.firstOrNull {
                it.fromState == from.index &&
                        it.toState == to.index &&
                        it.name == existingTransitionName
            }
        } else {
            null
        }

    var transitionName by remember { mutableStateOf(existingTransitionName ?: "") }
    var popSymbol by remember { mutableStateOf(existingPdaTransition?.pop ?: "") }
    var pushSymbol by remember { mutableStateOf(existingPdaTransition?.push ?: "") }
    var fromState: State by remember { mutableStateOf(from) }
    var toState: State by remember { mutableStateOf(to) }

    DefaultDialog(
        title = null,
        onDismiss = onDismiss,
        onConfirm = {
            if (this@TransitionDialog is FiniteMachine) {
                if (existingTransitionName == null) {
                    addNewTransition(
                        name = transitionName,
                        fromState = fromState,
                        toState = toState,
                    )
                } else {
                    transitions
                        .firstOrNull { t -> t.fromState == from.index && t.toState == to.index }
                        ?.let { it.name = transitionName }
                }
            } else if (this@TransitionDialog is PushdownMachine) {
                if (existingTransitionName == null) {
                    addNewTransition(
                        name = transitionName,
                        fromState = fromState,
                        toState = toState,
                        pop = popSymbol,
                        push = pushSymbol,
                    )
                } else {
                    pdaTransitions
                        .firstOrNull { t ->
                            t.fromState == from.index &&
                                    t.toState == to.index &&
                                    t.name == existingTransitionName
                        }?.let {
                            it.name = transitionName
                            it.pop = popSymbol
                            it.push = pushSymbol
                        }
                }
            }
            onConfirm()
        },
    ) {
        // Transition name field - labeled "read" for PDA
        DefaultTextField(
            label = if (this@TransitionDialog is PushdownMachine)
                stringResource(R.string.transition_read)
            else
                stringResource(R.string.transition_name),
            value = transitionName,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { transitionName = it },
        )

        // Pop and Push fields (PDA only) - separate lines
        if (this@TransitionDialog is PushdownMachine) {
            DefaultTextField(
                label = stringResource(R.string.stack_pop),
                value = popSymbol,
                onValueChange = { popSymbol = it },
                modifier = Modifier.fillMaxWidth(),
            )
            DefaultTextField(
                label = stringResource(R.string.stack_push),
                value = pushSymbol,
                onValueChange = { pushSymbol = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }

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
                label = "",
                defaultSelectedIndex = states.indexOf(from),
                modifier = Modifier.weight(1f),
            ) {
                if (it != fromState) fromState = it
            }
            Text(
                text = stringResource(R.string.to_state),
                style = MaterialTheme.typography.bodyMedium,
            )
            DropdownSelector(
                items = states,
                label = "",
                defaultSelectedIndex = states.indexOf(to),
                modifier = Modifier.weight(1f),
            ) {
                if (it != toState) toState = it
            }
        }
    }
}
