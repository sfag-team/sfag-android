package com.sfag.automata.ui.component.simulation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sfag.R
import com.sfag.automata.domain.model.machine.PushDownMachine

/** PDA stack bar. Position: BOTTOM. Shows stack contents left-to-right (bottom-to-top). */
@Composable
fun PushDownMachine.PushDownStackBar() {
    PushDownStackBar(stack = symbolStack)
}

@Composable
fun PushDownStackBar(stack: List<Char>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TAPE_BAR_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier.size(CELL_SIZE),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.stack),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(CELL_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(stack) { symbol ->
                TapeCell(symbol = symbol)
            }
        }
    }
}
