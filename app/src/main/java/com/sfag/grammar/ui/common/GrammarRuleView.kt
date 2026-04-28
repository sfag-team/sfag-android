package com.sfag.grammar.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val noOpInteraction = remember { MutableInteractionSource() }
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = grammarRule.left,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .clickable(
                            interactionSource = noOpInteraction,
                            indication = null,
                            onClick = { if (!isGrammarFinished) onEdit() },
                        )
            )
        }
        Text(Symbols.ARROW, style = MaterialTheme.typography.headlineMedium)
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = grammarRule.right,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .clickable(
                            interactionSource = noOpInteraction,
                            indication = null,
                            onClick = { if (!isGrammarFinished) onEdit() },
                        )
            )
        }
        if (!isGrammarFinished) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                IconButton(onClick = { onEdit() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Create,
                        contentDescription = stringResource(R.string.edit_rule),
                    )
                }
                IconButton(
                    onClick = { grammarViewModel.removeRule(grammarRule) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.remove_rule),
                    )
                }
            }
        }
    }
}
