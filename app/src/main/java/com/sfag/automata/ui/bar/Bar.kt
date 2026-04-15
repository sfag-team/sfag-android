package com.sfag.automata.ui.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.sfag.automata.ui.machine.cellPadding
import com.sfag.automata.ui.machine.cellSize

/**
 * Layout for the tape bar: a label slot on the left and a cells slot on the right. `cellsWidth`
 * is sized to fit a whole number of cells exactly (including inter-cell spacing) so the rightmost
 * cell lines up with the stack's rightmost cell below. Any leftover width is absorbed by the
 * label slot.
 */
@Composable
internal fun BarRow(
    label: @Composable (width: Dp) -> Unit,
    cells: @Composable (width: Dp) -> Unit,
) {
    val cellStep = cellSize + cellPadding
    BoxWithConstraints(
        modifier =
            Modifier.fillMaxWidth()
                .height(cellSize + cellPadding * 2)
                .padding(horizontal = cellPadding)
    ) {
        val innerWidth = maxWidth
        val cellsToShow = ((innerWidth - cellSize) / cellStep).toInt().coerceAtLeast(1)
        val cellsWidth = cellStep * cellsToShow - cellPadding
        val labelWidth = innerWidth - cellPadding - cellsWidth

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(cellPadding),
        ) {
            label(labelWidth)
            cells(cellsWidth)
        }
    }
}
