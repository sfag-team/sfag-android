package com.sfag.automata.ui.component.simulation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sfag.automata.domain.model.machine.Machine

/** Input tape bar for FA and PDA simulation. Position: TOP. */
@Composable
fun Machine.InputTapeBar(listState: LazyListState, onEditClick: () -> Unit) {
    val fullWord = fullInputSnapshot.ifEmpty { input.toString() }
    val consumed = (fullWord.length - imuInput.length).coerceIn(0, fullWord.length)
    val headIndex = consumed
    // Stable list reference: avoids LazyRow re-rendering all cells on every recompose
    val symbols = remember(fullWord) { fullWord.toList() }

    TapeBar(
        symbols = symbols,
        headIndex = headIndex,
        listState = listState,
        headColor = MaterialTheme.colorScheme.primary,
        showConsumed = true,
        onEditClick = onEditClick,
        infiniteRight = true
    )
}
