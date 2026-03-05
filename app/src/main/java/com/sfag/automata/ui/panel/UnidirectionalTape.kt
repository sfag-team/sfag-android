package com.sfag.automata.ui.panel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

import com.sfag.automata.model.machine.Machine

/** Unidirectional tape for FA and PDA simulation. Position: TOP. */
@Composable
fun Machine.UnidirectionalTape(listState: LazyListState, onEditClick: () -> Unit) {
    val fullWord = fullInputSnapshot.ifEmpty { input.toString() }
    val headIndex = (fullWord.length - imuInput.length).coerceIn(0, fullWord.length)
    val symbols = fullWord.toList()

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
