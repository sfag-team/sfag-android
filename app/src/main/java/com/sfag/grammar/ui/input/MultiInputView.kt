package com.sfag.grammar.ui.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.grammar.domain.grammar.GrammarRule
import com.sfag.grammar.domain.grammar.GrammarType
import com.sfag.grammar.domain.grammar.ParseResult
import com.sfag.grammar.domain.grammar.cykAccepts
import com.sfag.grammar.domain.grammar.parse
import com.sfag.grammar.ui.GrammarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MultiInputView(
    grammarViewModel: GrammarViewModel,
    inputsViewModel: MultiInputViewModel,
    onTestInput: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inputs = inputsViewModel.inputs
    val rules = grammarViewModel.getIndividualRules()
    val terminals = grammarViewModel.terminals
    val grammarType = grammarViewModel.grammarType

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.test_multiple_inputs),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier,
        )
        HorizontalDivider(
            modifier = Modifier.height(4.dp).fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
        )
        // Compute fresh every composition (not cached) so indices are never stale
        val rowsToRemove =
            if (inputs.size > 5) {
                inputs.indices.filter { it != inputs.lastIndex && inputs[it] == "" }
            } else {
                emptyList()
            }
        SideEffect { rowsToRemove.asReversed().forEach { inputsViewModel.removeRowAt(it) } }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(inputs.indices.toList()) { index ->
                if (index in rowsToRemove) {
                    return@items
                }
                TableRow(
                    inputText = inputs[index],
                    onValueChange = { newText ->
                        inputsViewModel.updateRowText(index, newText)
                        if (index == inputs.lastIndex && newText.isNotBlank()) {
                            inputsViewModel.addRow()
                        }
                    },
                    rules = rules,
                    terminals = terminals,
                    grammarType = grammarType,
                    onTestInput = onTestInput,
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    inputText: String,
    onValueChange: (String) -> Unit,
    rules: List<GrammarRule>,
    terminals: Set<Char>,
    grammarType: GrammarType,
    onTestInput: (String) -> Unit,
) {
    // null = loading, true = accepted, false = rejected/inconclusive
    var isValid by remember { mutableStateOf<Boolean?>(null) }
    var isInconclusive by remember { mutableStateOf(false) }

    LaunchedEffect(inputText, rules, terminals, grammarType) {
        isValid = null
        isInconclusive = false
        val isAccepted =
            withContext(Dispatchers.Default) {
                if (inputText.isNotEmpty() && inputText.any { it !in terminals }) {
                    false
                } else if (
                    grammarType == GrammarType.CONTEXT_FREE || grammarType == GrammarType.REGULAR
                ) {
                    cykAccepts(inputText, rules)
                } else {
                    when (parse(inputText, rules, terminals, grammarType)) {
                        is ParseResult.Success -> true
                        is ParseResult.Rejected -> false
                        is ParseResult.Inconclusive -> null
                    }
                }
            }
        if (isAccepted == null) {
            isInconclusive = true
        }
        isValid = isAccepted
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = inputText,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors =
                TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        )

        Spacer(modifier = Modifier.width(8.dp))

        when {
            isValid == null && !isInconclusive ->
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)

            isInconclusive ->
                Text(
                    text = "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

            isValid == true -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.valid_result),
                    tint = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = { onTestInput(inputText) }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.see_derivation),
                    )
                }
            }

            else ->
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.invalid_result),
                    tint = MaterialTheme.colorScheme.error,
                )
        }
    }
}
