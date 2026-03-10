package com.sfag.grammar.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.grammar.domain.grammar.GrammarType

@Composable
fun FormalDefinitionView(
    nonTerminals: Set<Char>,
    terminals: Set<Char>,
    grammarType: GrammarType?,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "G = (N, T, P, S)",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.start_symbol),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
        )
        Text(
            text = "S",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.non_terminal),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
        )
        Text(
            text =
                if (nonTerminals.isNotEmpty()) {
                    "N = ${nonTerminals.joinToString(", ", "{", "}")}"
                } else {
                    "N = {}"
                },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.grammar_terminal),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
        )
        Text(
            text =
                if (terminals.isNotEmpty()) {
                    "T = ${terminals.joinToString(", ", "{", "}")}"
                } else {
                    "T = {}"
                },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.grammar_type),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = grammarType.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
