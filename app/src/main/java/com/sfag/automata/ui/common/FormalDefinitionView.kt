package com.sfag.automata.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sfag.R
import com.sfag.automata.domain.common.FormalDefinition
import com.sfag.main.config.Symbols

/** Renders the formal mathematical definition of an automaton. */
@Composable
fun FormalDefinitionView(definition: FormalDefinition) {
    val typeLabel =
        stringResource(
            when (definition) {
                is FormalDefinition.Finite -> R.string.fa_label
                is FormalDefinition.Pushdown -> R.string.pda_label
                is FormalDefinition.Turing -> R.string.tm_label
            }
        )
    Column(modifier = Modifier.padding(16.dp)) {
        Text(definition.tupleHeader(typeLabel), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        definition.componentLines().forEach { line ->
            Text(line, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        definition.transitionLabels.forEach { description ->
            Text(text = description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun FormalDefinition.tupleHeader(typeLabel: String): String {
    val tuple = buildList {
        add("Q")
        add(Symbols.SIGMA)
        if (
            this@tupleHeader is FormalDefinition.Pushdown ||
                this@tupleHeader is FormalDefinition.Turing
        ) {
            add(Symbols.GAMMA)
        }
        add(Symbols.DELTA)
        add(initialStateName)
        when (this@tupleHeader) {
            is FormalDefinition.Finite -> Unit
            is FormalDefinition.Pushdown -> add(initialStackSymbol.toString())
            is FormalDefinition.Turing -> add(Symbols.BLANK)
        }
        add("F")
    }
    return "$typeLabel = (${tuple.joinToString(", ")})"
}

private fun FormalDefinition.componentLines(): List<String> = buildList {
    add("Q = { ${stateNames.joinToString(", ")} }")
    add("${Symbols.SIGMA} = { ${inputAlphabet.joinToString(", ")} }")
    when (this@componentLines) {
        is FormalDefinition.Finite -> Unit
        is FormalDefinition.Pushdown -> {
            add("${Symbols.GAMMA} = { ${stackAlphabet.joinToString(", ")} }")
            add("Z = '$initialStackSymbol'")
        }

        is FormalDefinition.Turing ->
            add("${Symbols.GAMMA} = { ${tapeAlphabet.joinToString(", ")} }")
    }
    add("F = { ${finalStateNames.joinToString(", ")} }")
}
