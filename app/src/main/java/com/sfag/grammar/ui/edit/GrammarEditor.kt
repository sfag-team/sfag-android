package com.sfag.grammar.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.grammar.domain.grammar.GrammarRule
import com.sfag.grammar.ui.GrammarViewModel
import com.sfag.grammar.ui.common.FormalDefinitionView
import com.sfag.grammar.ui.common.GrammarRuleView
import com.sfag.main.config.Symbols
import com.sfag.main.ui.component.DefaultButton
import kotlinx.coroutines.launch

@Composable
fun GrammarEditor(
    grammarViewModel: GrammarViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val rules = grammarViewModel.rules
    val nonTerminals = grammarViewModel.nonTerminals
    val terminals = grammarViewModel.terminals
    val grammarType = grammarViewModel.grammarType

    // State variables for new rule input
    var newLeft by remember { mutableStateOf(TextFieldValue("")) }
    var newRight by remember { mutableStateOf(TextFieldValue("")) }

    // Track which TextField is focused
    var focusedField by remember { mutableStateOf("") }
    var editingRule by remember { mutableStateOf<GrammarRule?>(null) }
    var editLeft by remember { mutableStateOf(TextFieldValue("")) }
    var editRight by remember { mutableStateOf(TextFieldValue("")) }
    val isGrammarFinished = grammarViewModel.isGrammarFinished

    val scope = rememberCoroutineScope()
    val invalidCharsError = stringResource(R.string.invalid_chars_error)
    val nonTerminalMissingError = stringResource(R.string.non_terminal_missing)

    Column(modifier = modifier.fillMaxWidth()) {
        // LazyColumn for displaying rules and input fields
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.rules_p),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(rules) { rule ->
                if (editingRule == rule) {
                    AddRule(
                        leftText = editLeft,
                        rightText = editRight,
                        focusedField = focusedField,
                        snackbarHostState = snackbarHostState,
                        onLeftChange = { editLeft = it },
                        onRightChange = { editRight = it },
                        onFieldFocusChange = { focusedField = it },
                        onAddRule = {
                            if (editLeft.text.isNotBlank() && editRight.text.isNotBlank()) {
                                grammarViewModel.updateRule(rule, editLeft.text, editRight.text)
                                editingRule = null
                            }
                        },
                    )
                } else {
                    GrammarRuleView(
                        grammarRule = rule,
                        grammarViewModel = grammarViewModel,
                        isGrammarFinished,
                        onEdit = {
                            editingRule = rule
                            editLeft = TextFieldValue(rule.lhs)
                            editRight = TextFieldValue(rule.rhs)
                        },
                    )
                }
            }

            // Input fields for adding a new rule
            item {
                if (!isGrammarFinished) {
                    AddRule(
                        leftText = newLeft,
                        rightText = newRight,
                        focusedField = focusedField,
                        snackbarHostState = snackbarHostState,
                        onLeftChange = { newLeft = it },
                        onRightChange = { newRight = it },
                        onFieldFocusChange = { focusedField = it },
                        onAddRule = {
                            if (newLeft.text.isNotBlank() && newRight.text.isNotBlank()) {
                                grammarViewModel.addRule(newLeft.text, newRight.text)
                                newLeft = TextFieldValue("")
                                newRight = TextFieldValue("")
                            }
                        },
                    )
                }
                DefaultButton(
                    text =
                        if (isGrammarFinished) stringResource(R.string.edit_grammar)
                        else stringResource(R.string.editing_done),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    onClick = {
                        val editing = editingRule
                        if (editing == null) {
                            grammarViewModel.toggleGrammarFinished()
                        } else {
                            val validChars =
                                editLeft.text.all {
                                    it.isLetterOrDigit() || it == '|' || "$it" == Symbols.EPSILON
                                } &&
                                    editRight.text.all {
                                        it.isLetterOrDigit() ||
                                            it == '|' ||
                                            "$it" == Symbols.EPSILON
                                    }
                            if (!validChars) {
                                scope.launch { snackbarHostState.showSnackbar(invalidCharsError) }
                            } else if (!editLeft.text.any { it.isUpperCase() }) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(nonTerminalMissingError)
                                }
                            } else if (editLeft.text.isNotBlank() && editRight.text.isNotBlank()) {
                                grammarViewModel.updateRule(editing, editLeft.text, editRight.text)
                                editingRule = null
                                grammarViewModel.toggleGrammarFinished()
                            }
                        }
                    },
                )
            }
        }
        Box(
            modifier =
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            FormalDefinitionView(nonTerminals, terminals, grammarType)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AddRule(
    leftText: TextFieldValue,
    rightText: TextFieldValue,
    focusedField: String,
    snackbarHostState: SnackbarHostState,
    onLeftChange: (TextFieldValue) -> Unit,
    onRightChange: (TextFieldValue) -> Unit,
    onFieldFocusChange: (String) -> Unit,
    onAddRule: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val invalidCharsError = stringResource(R.string.invalid_chars_error)
    val nonTerminalMissingError = stringResource(R.string.non_terminal_missing)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = leftText,
            onValueChange = { onLeftChange(it) },
            placeholder = { Text(stringResource(R.string.left_side)) },
            singleLine = true,
            modifier =
                Modifier.weight(1f).onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFieldFocusChange("left")
                    }
                },
        )
        Text(Symbols.PRODUCTION, style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = rightText,
            onValueChange = { onRightChange(it) },
            placeholder = { Text(stringResource(R.string.right_side)) },
            singleLine = true,
            modifier =
                Modifier.weight(1f).onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFieldFocusChange("right")
                    }
                },
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.height(56.dp).width(42.dp),
        ) {
            SymbolInsertButton(
                symbol = "|",
                modifier = Modifier.fillMaxWidth().weight(1f),
                onClick = {
                    when (focusedField) {
                        "left" ->
                            onLeftChange(
                                TextFieldValue(
                                    leftText.text + "|",
                                    TextRange(leftText.text.length + 1),
                                )
                            )

                        "right" ->
                            onRightChange(
                                TextFieldValue(
                                    rightText.text + "|",
                                    TextRange(rightText.text.length + 1),
                                )
                            )
                    }
                },
            )
            SymbolInsertButton(
                symbol = Symbols.EPSILON,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onClick = {
                    when (focusedField) {
                        "left" ->
                            onLeftChange(
                                TextFieldValue(
                                    leftText.text + Symbols.EPSILON,
                                    TextRange(leftText.text.length + 1),
                                )
                            )

                        "right" ->
                            onRightChange(
                                TextFieldValue(
                                    rightText.text + Symbols.EPSILON,
                                    TextRange(rightText.text.length + 1),
                                )
                            )
                    }
                },
            )
        }

        Box(
            modifier =
                Modifier.height(56.dp).width(42.dp).clickable {
                    val validChars =
                        leftText.text.all {
                            it.isLetterOrDigit() || it == '|' || "$it" == Symbols.EPSILON
                        } &&
                            rightText.text.all {
                                it.isLetterOrDigit() || it == '|' || "$it" == Symbols.EPSILON
                            }
                    if (!validChars) {
                        scope.launch { snackbarHostState.showSnackbar(invalidCharsError) }
                    } else if (!leftText.text.any { it.isUpperCase() }) {
                        scope.launch { snackbarHostState.showSnackbar(nonTerminalMissingError) }
                    } else {
                        onAddRule()
                        focusManager.clearFocus()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = stringResource(R.string.add_rule),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Filled tonal helper button that inserts [symbol] into the focused field. */
@Composable
private fun SymbolInsertButton(symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
