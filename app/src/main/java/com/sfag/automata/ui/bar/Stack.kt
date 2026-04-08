package com.sfag.automata.ui.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.ui.machine.cellPadding
import com.sfag.automata.ui.machine.cellSize

/** PDA stack bar. Shows stack contents left-to-right (bottom-to-top). */
@Composable
fun PushdownMachine.Stack(overrideSymbols: List<Char>? = null) {
    val displaySymbols = overrideSymbols ?: symbolStack
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(cellSize + cellPadding * 2)
                .padding(horizontal = cellPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(cellPadding),
    ) {
        Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.pda_stack),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(cellPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (displaySymbols.isEmpty()) {
                item { Cell(symbol = ' ') }
            } else {
                items(displaySymbols) { Cell(symbol = it) }
            }
        }
    }
}
