package com.sfag.automata.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.launch

private enum class StackOperation {
    POP,
    PUSH,
}

private class PdaDialogState(
    val popSymbol: MutableState<String>,
    val pushSymbol: MutableState<String>,
    val stackTopSymbol: MutableState<String>,
    val stackOperation: MutableState<StackOperation?>,
)

/** Dialog for creating or editing a transition. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Machine.TransitionDialog(
    from: State,
    to: State,
    existingTransitionName: String?,
    onDismiss: () -> Unit,
) {
    var transitionName by remember { mutableStateOf(existingTransitionName ?: "") }
    var fromState: State by remember { mutableStateOf(from) }
    var toState: State by remember { mutableStateOf(to) }

    // PDA-specific state - only initialized for PushdownMachine
    val pdaState =
        if (this@TransitionDialog is PushdownMachine) {
            val popSymbol =
                remember {
                    mutableStateOf(
                        pdaTransitions
                            .firstOrNull {
                                it.fromState == from.index &&
                                    it.toState == to.index &&
                                    it.name == existingTransitionName
                            }?.pop ?: "",
                    )
                }
            val pushSymbol =
                remember {
                    mutableStateOf(
                        pdaTransitions
                            .firstOrNull {
                                it.fromState == from.index &&
                                    it.toState == to.index &&
                                    it.name == existingTransitionName
                            }?.push ?: "",
                    )
                }
            val stackTopSymbol = remember { mutableStateOf("") }
            val stackOperation = remember { mutableStateOf<StackOperation?>(null) }

            LaunchedEffect(Unit) {
                if (existingTransitionName != null) {
                    if (pushSymbol.value.isNotEmpty()) {
                        pushSymbol.value = pushSymbol.value.dropLast(1)
                        stackTopSymbol.value = popSymbol.value
                        stackOperation.value = StackOperation.PUSH
                    } else {
                        stackOperation.value = StackOperation.POP
                    }
                }
            }

            PdaDialogState(popSymbol, pushSymbol, stackTopSymbol, stackOperation)
        } else {
            null
        }

    DefaultDialog(
        title = null,
        onDismiss = onDismiss,
        onConfirm = {
            if (this@TransitionDialog is FiniteMachine) {
                if (existingTransitionName == null) {
                    addNewTransition(name = transitionName, fromState = fromState, toState = toState)
                } else {
                    transitions
                        .firstOrNull { t -> t.fromState == from.index && t.toState == to.index }
                        ?.let { it.name = transitionName }
                }
            } else if (this@TransitionDialog is PushdownMachine && pdaState != null) {
                if (existingTransitionName == null) {
                    addNewTransition(
                        name = transitionName,
                        fromState = fromState,
                        toState = toState,
                        pop = pdaState.popSymbol.value,
                        stackTop = pdaState.stackTopSymbol.value,
                        push = pdaState.pushSymbol.value,
                    )
                } else {
                    pdaTransitions
                        .firstOrNull { t ->
                            t.fromState == from.index &&
                                t.toState == to.index &&
                                t.name == existingTransitionName
                        }?.let {
                            it.name = transitionName
                            if (pdaState.stackOperation.value == StackOperation.POP) {
                                it.pop = pdaState.popSymbol.value
                                it.push = ""
                            } else {
                                it.pop = pdaState.stackTopSymbol.value
                                it.push = pdaState.pushSymbol.value + pdaState.stackTopSymbol.value
                            }
                        }
                }
            }
            onDismiss()
        },
    ) {
        // Stack operation section (PDA only) - on top
        if (this@TransitionDialog is PushdownMachine && pdaState != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.stack_operation),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val tooltipState = rememberTooltipState()
                val scope = rememberCoroutineScope()
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above,
                        ),
                    tooltip = {
                        PlainTooltip {
                            Column {
                                Text(stringResource(R.string.help_pop))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.help_push))
                            }
                        }
                    },
                    state = tooltipState,
                ) {
                    IconButton(
                        onClick = { scope.launch { tooltipState.show() } },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.help),
                            contentDescription = stringResource(R.string.stack_operation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            val popLabel = stringResource(R.string.stack_pop)
            val pushLabel = stringResource(R.string.stack_push)
            DropdownSelector(
                items = listOf(popLabel, pushLabel),
                defaultSelectedIndex =
                    if (pdaState.stackOperation.value == StackOperation.POP) 0 else 1,
                modifier = Modifier.fillMaxWidth(),
            ) { selected ->
                pdaState.stackOperation.value =
                    if (selected == popLabel) StackOperation.POP else StackOperation.PUSH
                if (pdaState.stackOperation.value == StackOperation.POP) {
                    pdaState.stackTopSymbol.value = ""
                    pdaState.pushSymbol.value = ""
                } else {
                    pdaState.popSymbol.value = ""
                }
            }

            // Fixed height box to prevent dialog bouncing when switching modes
            Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                if (pdaState.stackOperation.value == StackOperation.POP) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DefaultTextField(
                            label = stringResource(R.string.stack_pop),
                            value = pdaState.popSymbol.value,
                            onValueChange = {
                                pdaState.popSymbol.value = it
                                pdaState.pushSymbol.value = ""
                                pdaState.stackTopSymbol.value = ""
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            pdaState.popSymbol.value.length == 1
                        }
                    }
                } else if (pdaState.stackOperation.value == StackOperation.PUSH) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DefaultTextField(
                            label = stringResource(R.string.pop_check),
                            value = pdaState.stackTopSymbol.value,
                            onValueChange = {
                                pdaState.stackTopSymbol.value = it
                                pdaState.popSymbol.value = ""
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            pdaState.stackTopSymbol.value.length == 1
                        }
                        DefaultTextField(
                            label = stringResource(R.string.stack_push),
                            value = pdaState.pushSymbol.value,
                            onValueChange = {
                                pdaState.pushSymbol.value = it
                                pdaState.popSymbol.value = ""
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            pdaState.pushSymbol.value.length <= 1
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Transition name field
        DefaultTextField(
            label = stringResource(R.string.transition_name),
            value = transitionName,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { transitionName = it },
        )

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
