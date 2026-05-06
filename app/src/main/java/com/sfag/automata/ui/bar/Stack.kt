package com.sfag.automata.ui.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sfag.automata.ui.machine.cellPadding
import com.sfag.automata.ui.machine.cellSize

/** PDA stack bar. Bottom symbol on the right; stack grows to the left. */
@Composable
fun Stack(symbols: List<Char>) {
    LazyRow(
        modifier =
            Modifier.fillMaxWidth()
                .height(cellSize + cellPadding * 2)
                .padding(horizontal = cellPadding),
        horizontalArrangement = Arrangement.spacedBy(cellPadding, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (symbols.isEmpty()) {
            item { Cell(symbol = ' ') }
        } else {
            items(symbols.reversed()) { Cell(symbol = it) }
        }
    }
}
