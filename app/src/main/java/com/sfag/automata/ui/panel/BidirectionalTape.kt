package com.sfag.automata.ui.panel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable

import com.sfag.automata.model.machine.TuringMachine

/** Bidirectional tape bar for Turing machine simulation. Position: TOP. */
@Composable
fun TuringMachine.BidirectionalTape(listState: LazyListState, onEditClick: () -> Unit) {
    TapeBar(
        symbols = tape,
        headIndex = headPosition,
        listState = listState,
        onEditClick = onEditClick,
        infiniteRight = true,
        infiniteLeft = true
    )
}
