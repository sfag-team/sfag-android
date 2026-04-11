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

/**
 * Renders the formal mathematical definition of an automaton. Infers machine type from which
 * optional fields are present in FormalDefinition.
 */
@Composable
fun FormalDefinitionView(definition: FormalDefinition) {
    Column(modifier = Modifier.padding(16.dp)) {
        val headerText =
            when {
                definition.stackAlphabet != null ->
                    "M = (Q, ${Symbols.SIGMA}, ${Symbols.GAMMA}, ${Symbols.DELTA}, ${definition.initialStateName}, ${definition.initialStackSymbol ?: 'Z'}, F)"

                definition.tapeAlphabet != null ->
                    "M = (Q, ${Symbols.SIGMA}, ${Symbols.GAMMA}, ${Symbols.DELTA}, ${definition.initialStateName}, ${definition.blankSymbol ?: Symbols.BLANK}, F)"

                else ->
                    "M = (Q, ${Symbols.SIGMA}, ${Symbols.DELTA}, ${definition.initialStateName}, F)"
            }
        Text(headerText, style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        // State set
        Text(
            "Q = { ${definition.stateNames.joinToString(", ")} }",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            "${Symbols.SIGMA} = { ${definition.inputAlphabet.joinToString(", ")} }",
            style = MaterialTheme.typography.bodyLarge,
        )

        // PDA stack alphabet
        definition.stackAlphabet?.let { alphabet ->
            Text(
                "${Symbols.GAMMA} = { ${alphabet.joinToString(", ")} }",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Z = '${definition.initialStackSymbol ?: 'Z'}'",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        // Turing tape alphabet
        definition.tapeAlphabet?.let { alphabet ->
            Text(
                "${Symbols.GAMMA} = { ${alphabet.joinToString(", ")} }",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "${Symbols.BLANK} = '${definition.blankSymbol ?: Symbols.BLANK}'",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        // Final states
        Text(
            "F = { ${definition.finalStateNames.joinToString(", ")} }",
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(modifier = Modifier.height(8.dp))

        definition.transitionDescriptions.forEach { description ->
            Text(text = description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
