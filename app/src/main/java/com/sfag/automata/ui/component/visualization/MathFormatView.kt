package com.sfag.automata.ui.component.visualization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Text(headerText, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(12.dp))

        // State set
        Text("Q = { ${data.stateNames.joinToString(", ")} }")

        Text("${Symbols.SIGMA} = { ${data.inputAlphabet.joinToString(", ")} }")

        when (data.machineType) {
            MachineType.Pushdown -> {
                data.stackAlphabet?.let { alphabet ->
                    Text("${Symbols.GAMMA} = { ${alphabet.joinToString(", ")} }")
                }
                Text("Z = '${data.initialStackSymbol ?: 'Z'}'")
            }
            MachineType.Turing -> {
                data.tapeAlphabet?.let { alphabet ->
                    Text("${Symbols.GAMMA} = { ${alphabet.joinToString(", ")} }")
                }
                Text("${Symbols.BLANK} = '${data.blankSymbol ?: Symbols.BLANK}'")
            }
            MachineType.Finite -> { /* No additional alphabets */ }
        }

        // Final states
        Text("F = { ${data.finalStateNames.joinToString(", ")} }")

        Spacer(modifier = Modifier.height(12.dp))

        Text("${Symbols.DELTA}:", fontWeight = FontWeight.Bold)
        Text(
            text = data.transitionDescriptions.joinToString("\n"),
            fontFamily = FontFamily.Monospace
        )
    }
}
