package com.sfag.automata.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.sfag.R
import com.sfag.automata.model.machine.AcceptanceCriteria
import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.machine.MachineType
import com.sfag.automata.model.machine.PushdownMachine
import com.sfag.shared.ui.component.DefaultButton
import com.sfag.shared.ui.component.DefaultTextField
import com.sfag.shared.ui.component.DropDownSelector
import com.sfag.shared.ui.component.ImmutableTextField
import com.sfag.shared.ui.theme.extendedColorScheme

/**
 * Returns whether the given input is accepted by this machine.
 */
private fun Machine.isAccepted(input: StringBuilder): Boolean {
    if (machineType == MachineType.Pushdown) {
        this as PushdownMachine
        return if (acceptanceCriteria == AcceptanceCriteria.BY_FINAL_STATE) {
            canReachFinalState(input, true)
        } else {
            canReachInitialStackPDA(input, true, listOf('Z'))
        }
    }
    return canReachFinalState(input, true)
}

/**
 * Full-screen input editor.
 *
 * Layout: text field (top) -> buttons (middle) -> saved inputs (bottom, scrollable)
 *
 * @param onConfirm called when user confirms changes (saves state)
 * @param onDiscard called when user discards changes (original state restored internally)
 */

@Composable
fun Machine.EditingInput(onConfirm: () -> Unit, onDiscard: () -> Unit) {
    // Capture original state once on first composition
    val originalInput = remember { input.toString() }
    val originalSavedInputs = remember { savedInputs.map { StringBuilder(it.toString()) }.toMutableList() }
    val originalCriteria = remember {
        if (machineType == MachineType.Pushdown) (this as PushdownMachine).acceptanceCriteria else null
    }

    var editingRecompose by remember { mutableIntStateOf(0) }

    val acceptedFill = MaterialTheme.extendedColorScheme.accepted.colorContainer
    val rejectedFill = MaterialTheme.extendedColorScheme.rejected.colorContainer
    val acceptedColor = MaterialTheme.extendedColorScheme.accepted.onColorContainer
    val rejectedColor = MaterialTheme.extendedColorScheme.rejected.onColorContainer

    key(editingRecompose) {
        val inputValue = remember { mutableStateOf(input.toString()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TOP: PDA acceptance criteria + text field card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PDA acceptance criteria
                if (machineType == MachineType.Pushdown) {
                    this@EditingInput as PushdownMachine
                    val listOfCriteria = listOf(
                        AcceptanceCriteria.BY_FINAL_STATE.text,
                        AcceptanceCriteria.BY_INITIAL_STACK.text
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = stringResource(R.string.accept_by), style = MaterialTheme.typography.bodyLarge)
                        DropDownSelector(
                            items = listOfCriteria,
                            defaultSelectedIndex = listOfCriteria.indexOf(acceptanceCriteria.text),
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge
                        ) { newCriteria ->
                            acceptanceCriteria =
                                if (newCriteria.toString() == AcceptanceCriteria.BY_FINAL_STATE.text) AcceptanceCriteria.BY_FINAL_STATE else AcceptanceCriteria.BY_INITIAL_STACK
                        }
                    }
                }

                // Text field + add button
                val validationColor = if (inputValue.value.isEmpty()) null
                else if (isAccepted(StringBuilder(inputValue.value))) acceptedColor
                else rejectedColor

                DefaultTextField(
                    modifier = Modifier.fillMaxWidth(),
                    hint = stringResource(R.string.tape_input),
                    value = inputValue.value,
                    labelColor = validationColor,
                    textColor = validationColor,
                    onTextChange = { newInput ->
                        input.clear()
                        input.append(newInput)
                        inputValue.value = newInput
                        imuInput = StringBuilder(input.toString())
                        fullInputSnapshot = input.toString()
                    },
                    trailingIcon = if (inputValue.value.isNotEmpty()) {
                        {
                            IconButton(onClick = {
                                savedInputs.add(input)
                                input = java.lang.StringBuilder(savedInputs.last().toString() + "")
                                editingRecompose++
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.add),
                                    contentDescription = stringResource(R.string.add_input),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else null
                )
            }

            // MIDDLE: Save / Discard buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        // Restore original state
                        input = StringBuilder(originalInput)
                        imuInput = StringBuilder(originalInput)
                        fullInputSnapshot = originalInput
                        savedInputs.clear()
                        savedInputs.addAll(originalSavedInputs.map { StringBuilder(it.toString()) })
                        if (originalCriteria != null) {
                            (this@EditingInput as PushdownMachine).acceptanceCriteria = originalCriteria
                        }
                        setInitialStateAsCurrent()
                        onDiscard()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.discard))
                }
                DefaultButton(
                    text = stringResource(R.string.save),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        setInitialStateAsCurrent()
                        onConfirm()
                    }
                )
            }

            // BOTTOM: Saved inputs card (scrollable, takes remaining space)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                if (savedInputs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_saved_inputs),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .then(if (savedInputs.isEmpty()) Modifier else Modifier.weight(1f))
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                ) {
                    items(savedInputs.toList()) { savedInput ->
                        val accepted = isAccepted(savedInput)
                        ImmutableTextField(
                            text = savedInput.toString(),
                            backgroundColor = if (accepted) acceptedFill else rejectedFill,
                            textColor = if (accepted) acceptedColor else rejectedColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            onClick = {
                                this@EditingInput.input = StringBuilder(savedInput.toString())
                                imuInput = StringBuilder(savedInput.toString())
                                fullInputSnapshot = this@EditingInput.input.toString()
                                setInitialStateAsCurrent()
                                editingRecompose++
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        savedInputs.remove(savedInput)
                                        editingRecompose++
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.delete),
                                        contentDescription = stringResource(R.string.delete),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
