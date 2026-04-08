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
                    it.read == existingTransitionName
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
        onDismissRequest = onDismiss,
        buttons = {
            CancelButton(onClick = onDismiss)
            ConfirmButton(
                onClick = {
                    val normalizedName = JffUtils.normalizeEpsilon(transitionName)
                    if (this@TransitionDialog is FiniteMachine) {
                        if (existingTransitionName == null) {
                            addNewTransition(
                                read = normalizedName,
                                fromState = fromState,
                                toState = toState,
                            )
                        } else {
                            transitions
                                .firstOrNull { t ->
                                    t.fromState == from.index && t.toState == to.index
                                }
                                ?.let { it.read = normalizedName }
                        }
                    } else if (this@TransitionDialog is PushdownMachine) {
                        val normalizedPop = JffUtils.normalizeEpsilon(popSymbol)
                        val normalizedPush = JffUtils.normalizeEpsilon(pushSymbol)
                        if (existingTransitionName == null) {
                            addNewTransition(
                                read = normalizedName,
                                fromState = fromState,
                                toState = toState,
                                pop = normalizedPop,
                                push = normalizedPush,
                            )
                        } else {
                            pdaTransitions
                                .firstOrNull { t ->
                                    t.fromState == from.index &&
                                        t.toState == to.index &&
                                        t.read == existingTransitionName
                                }
                                ?.let {
                                    it.read = normalizedName
                                    it.pop = normalizedPop
                                    it.push = normalizedPush
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
            value = transitionName,
            onValueChange = { transitionName = it },
            modifier = Modifier.fillMaxWidth(),
            label =
                if (this@TransitionDialog is PushdownMachine) {
                    stringResource(R.string.transition_read)
                } else {
                    stringResource(R.string.transition_name)
                },
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
    }
}
