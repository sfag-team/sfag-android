package com.sfag.automata.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sfag.automata.domain.common.FormalDefinition
import com.sfag.main.config.Symbols

/** Renders the formal mathematical definition of an automaton. */
@Composable
fun FormalDefinitionView(definition: FormalDefinition) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(definition.tupleHeader(), style = MaterialTheme.typography.titleLarge)
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

private fun FormalDefinition.tupleHeader(): String {
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
            is FormalDefinition.Turing -> add(blankSymbol.toString())
        }
        add("F")
    }
    return "M = (${tuple.joinToString(", ")})"
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

        is FormalDefinition.Turing -> {
            add("${Symbols.GAMMA} = { ${tapeAlphabet.joinToString(", ")} }")
            add("${Symbols.BLANK} = '$blankSymbol'")
        }
    }
    add("F = { ${finalStateNames.joinToString(", ")} }")
}
