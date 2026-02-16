package com.sfag.automata.presentation.component.visualization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sfag.automata.domain.model.machine.MachineFormatData
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.shared.util.Symbols

/**
 * Renders the formal mathematical definition of an automaton.
 */
@Composable
fun MathFormatView(data: MachineFormatData) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        val headerText = when (data.machineType) {
            MachineType.Finite -> "M = (Q, ${Symbols.SIGMA}, ${Symbols.DELTA}, ${data.initialStateName}, F)"
            MachineType.Pushdown -> "M = (Q, ${Symbols.SIGMA}, ${Symbols.GAMMA}, ${Symbols.DELTA}, ${data.initialStateName}, ${data.initialStackSymbol ?: 'Z'}, F)"
            MachineType.Turing -> "M = (Q, ${Symbols.GAMMA}, ${Symbols.DELTA}, ${data.initialStateName}, ${data.blankSymbol ?: Symbols.BLANK}, F)"
        }
        Text(headerText, style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // State set
        Text("Q = { ${data.stateNames.joinToString(", ")} }", style = MaterialTheme.typography.bodyLarge)

        Text("${Symbols.SIGMA} = { ${data.inputAlphabet.joinToString(", ")} }", style = MaterialTheme.typography.bodyLarge)

        when (data.machineType) {
            MachineType.Pushdown -> {
                data.stackAlphabet?.let { alphabet ->
                    Text("${Symbols.GAMMA} = { ${alphabet.joinToString(", ")} }", style = MaterialTheme.typography.bodyLarge)
                }
                Text("Z = '${data.initialStackSymbol ?: 'Z'}'", style = MaterialTheme.typography.bodyLarge)
            }
            MachineType.Turing -> {
                data.tapeAlphabet?.let { alphabet ->
                    Text("${Symbols.GAMMA} = { ${alphabet.joinToString(", ")} }", style = MaterialTheme.typography.bodyLarge)
                }
                Text("${Symbols.BLANK} = '${data.blankSymbol ?: Symbols.BLANK}'", style = MaterialTheme.typography.bodyLarge)
            }
            MachineType.Finite -> { /* No additional alphabets */ }
        }

        // Final states
        Text("F = { ${data.finalStateNames.joinToString(", ")} }", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Text("${Symbols.DELTA}:", style = MaterialTheme.typography.titleMedium)
        Text(
            text = data.transitionDescriptions.joinToString("\n"),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)
        )
    }
}
