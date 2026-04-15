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
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.ui.machine.cellPadding
import com.sfag.automata.ui.machine.cellSize

/** PDA stack bar. Bottom symbol on the right; stack grows to the left. */
@Composable
fun PushdownMachine.Stack(overrideSymbols: List<Char>? = null) {
    val displaySymbols = overrideSymbols ?: symbolStack

    LazyRow(
        modifier =
            Modifier.fillMaxWidth()
                .height(cellSize + cellPadding * 2)
                .padding(horizontal = cellPadding),
        horizontalArrangement = Arrangement.spacedBy(cellPadding, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (displaySymbols.isEmpty()) {
            item { Cell(symbol = ' ') }
        } else {
            items(displaySymbols.reversed()) { Cell(symbol = it) }
        }
    }
}
