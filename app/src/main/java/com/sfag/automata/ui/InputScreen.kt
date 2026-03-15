package com.sfag.automata.ui

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sfag.automata.domain.machine.AcceptanceCriteria
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.main.config.Symbols
import com.sfag.main.ui.component.DefaultButton
import com.sfag.main.ui.component.DefaultTextField
import com.sfag.main.ui.component.DropdownSelector
import com.sfag.main.ui.component.ImmutableTextField
import com.sfag.main.ui.theme.extendedColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Full-screen input editor. */
@Composable
fun Machine.InputScreen(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val savedInputs =
        remember {
            this@InputScreen.savedInputs.map { StringBuilder(it.toString()) }.toMutableList()
        }
    val criteria = remember { (this as? PushdownMachine)?.acceptanceCriteria }

    var recomposeKey by remember { mutableIntStateOf(0) }
    val newFullInput = remember { mutableStateOf(fullInput) }

    val acceptedFill = MaterialTheme.extendedColorScheme.accepted.colorContainer
    val rejectedFill = MaterialTheme.extendedColorScheme.rejected.colorContainer
    val acceptedColor = MaterialTheme.extendedColorScheme.accepted.onColorContainer
    val rejectedColor = MaterialTheme.extendedColorScheme.rejected.onColorContainer

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // TOP: PDA acceptance criteria + text field card
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // PDA acceptance criteria
            if (this@InputScreen is PushdownMachine) {
                val listOfCriteria =
                    listOf(
                        AcceptanceCriteria.BY_FINAL_STATE.text,
                        AcceptanceCriteria.BY_EMPTY_STACK.text,
                    )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.accept_by),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    DropdownSelector(
                        items = listOfCriteria,
                        defaultSelectedIndex = listOfCriteria.indexOf(acceptanceCriteria.text),
                        modifier = Modifier.weight(1f),
                    ) { newCriteria ->
                        acceptanceCriteria =
                            if (newCriteria == AcceptanceCriteria.BY_FINAL_STATE.text) {
                                AcceptanceCriteria.BY_FINAL_STATE
                            } else {
                                AcceptanceCriteria.BY_EMPTY_STACK
                            }
                    }
                }
            }

            // Text field + add button
            var isInputAccepted by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(newFullInput.value, recomposeKey) {
                val text = newFullInput.value
                isInputAccepted =
                    withContext(Dispatchers.Default) { isAccepted(StringBuilder(text)) }
            }
            val validationColor =
                when (isInputAccepted) {
                    true -> acceptedColor
                    false -> rejectedColor
                    null -> null
                }

            DefaultTextField(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.tape_input),
                value = newFullInput.value,
                labelColor = validationColor,
                textColor = validationColor,
                onValueChange = { value ->
                    newFullInput.value = value
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            this@InputScreen.savedInputs.add(StringBuilder(newFullInput.value))
                            recomposeKey++
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.add),
                            contentDescription = stringResource(R.string.add_input),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        }

        // MIDDLE: Save / Discard buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    // Restore original state
                    this@InputScreen.savedInputs.clear()
                    this@InputScreen.savedInputs.addAll(savedInputs.map { StringBuilder(it.toString()) })
                    if (criteria != null && this@InputScreen is PushdownMachine) {
                        acceptanceCriteria = criteria
                    }
                    onDismiss()
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.cancel_button))
            }
            DefaultButton(
                text = stringResource(R.string.confirm_button),
                modifier = Modifier.weight(1f),
                onClick = {
                    loadInput(newFullInput.value)
                    onConfirm()
                },
            )
        }

        // BOTTOM: Saved inputs card (scrollable, takes remaining space)
        // key() forces Compose to tear down and rebuild when savedInputs changes,
        // since savedInputs is a non-observable MutableList on the domain object
        key(recomposeKey) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                if (this@InputScreen.savedInputs.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_saved_inputs),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding =
                            PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                    ) {
                        items(this@InputScreen.savedInputs.toList()) { savedInput ->
                            val savedText = savedInput.toString()
                            var isAccepted by remember(savedText) { mutableStateOf<Boolean?>(null) }
                            LaunchedEffect(savedText) {
                                isAccepted =
                                    withContext(Dispatchers.Default) { isAccepted(savedInput) }
                            }
                            val bgColor =
                                when (isAccepted) {
                                    true -> acceptedFill
                                    false -> rejectedFill
                                    null -> MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            val textColor =
                                when (isAccepted) {
                                    true -> acceptedColor
                                    false -> rejectedColor
                                    null -> MaterialTheme.colorScheme.onSurface
                                }
                            ImmutableTextField(
                                text = savedText.ifEmpty { Symbols.EPSILON },
                                backgroundColor = bgColor,
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                onClick = {
                                    newFullInput.value = savedInput.toString()
                                },
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            this@InputScreen.savedInputs.remove(savedInput)
                                            recomposeKey++
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.delete),
                                            contentDescription = stringResource(R.string.remove_item),
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
