package com.sfag.automata.presentation.component.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.sfag.automata.domain.model.machine.PushDownMachine

/**
 * Pushdown automaton stack bar.
 *
 * Displays the stack contents at the BOTTOM of the simulation view.
 * Shows symbols from bottom to top of stack (left to right).
 * The stack is a LIFO structure separate from the input tape.
 *
 * Position: BOTTOM
 * Used by: PDA
 */
@Composable
fun PushDownMachine.PushDownStackBar() {
    PushDownStackBar(stack = symbolStack)
}

/**
 * Stack bar with explicit parameters.
 * Decoupled from PushDownMachine for reusability and testing.
 */
@Composable
fun PushDownStackBar(stack: List<Char>) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TAPE_BAR_HEIGHT)
                .clip(MaterialTheme.shapes.medium)
                .border(
                    TAPE_BORDER_WIDTH,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.shapes.medium
                )
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = TAPE_PADDING)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Stack label
            Text(
                text = "Stack:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(TAPE_PADDING))

            // Stack symbols (bottom to top, left to right)
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(stack) { symbol ->
                    StackCell(symbol = symbol)
                    Spacer(modifier = Modifier.width(CELL_SPACING))
                }
            }
        }
    }
}
