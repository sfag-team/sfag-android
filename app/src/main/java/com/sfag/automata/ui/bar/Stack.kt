package com.sfag.automata.ui.bar

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
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.ui.common.BAR_HEIGHT
import com.sfag.automata.ui.common.CELL_SIZE

/** PDA stack bar. Shows stack contents left-to-right (bottom-to-top). */
@Composable
fun PushdownMachine.Stack() {
    Row(
        modifier = Modifier.fillMaxWidth().height(BAR_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(modifier = Modifier.size(CELL_SIZE), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.pda_stack),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy((BAR_HEIGHT - CELL_SIZE) / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(symbolStack) { Cell(symbol = it) }
        }
    }
}
