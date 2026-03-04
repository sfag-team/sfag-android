package com.sfag.automata.ui.component.simulation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.sfag.automata.domain.model.machine.TuringMachine

/** Turing machine tape bar. Position: TOP. Head can move in both directions. */
@Composable
fun TuringMachine.TuringTapeBar(listState: LazyListState, onEditClick: () -> Unit) {
    TapeBar(
        symbols = tape,
        headIndex = headPosition,
        listState = listState,
        onEditClick = onEditClick,
        infiniteRight = true,
        infiniteLeft = true
    )
}
