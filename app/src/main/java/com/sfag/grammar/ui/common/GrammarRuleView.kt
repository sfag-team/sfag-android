package com.sfag.grammar.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.grammar.domain.grammar.GrammarRule
import com.sfag.grammar.ui.GrammarViewModel
import com.sfag.main.config.Symbols

@Composable
fun GrammarRuleView(
    grammarRule: GrammarRule,
    grammarViewModel: GrammarViewModel,
    isGrammarFinished: Boolean,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = grammarRule.left,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.weight(1f),
        )
        Text(
            Symbols.ARROW,
            style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
            value = grammarRule.right,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.weight(1f),
        )
        if (!isGrammarFinished) {
            IconButton(onClick = { onEdit() }) {
                Icon(Icons.Default.Create, contentDescription = stringResource(R.string.edit_rule))
            }
            IconButton(onClick = { grammarViewModel.removeRule(grammarRule) }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_rule))
            }
        }
    }
}
