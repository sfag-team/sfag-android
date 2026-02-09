package com.sfag.automata.presentation.component.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.sfag.R
import com.sfag.automata.domain.model.machine.TuringMachine

/**
 * Turing machine tape bar.
 *
 * Displays the tape at the TOP of the simulation view.
 * The head can move in BOTH directions (left and right).
 * The tape extends infinitely in both directions as needed.
 * Includes an edit icon to modify the initial tape content.
 *
 * Position: TOP (replaces InputTapeBar for Turing machines)
 * Used by: Turing Machine
 */
@Composable
fun TuringMachine.TuringTapeBar(onEditClick: () -> Unit) {
    TuringTapeBar(
        tape = tape,
        headPosition = headPosition,
        onEditClick = onEditClick
    )
}

/**
 * Turing tape bar with explicit parameters.
 * Decoupled from TuringMachine for reusability and testing.
 */
@Composable
fun TuringTapeBar(
    tape: List<Char>,
    headPosition: Int,
    onEditClick: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()

    // Auto-scroll to keep head visible
    LaunchedEffect(headPosition) {
        if (tape.isNotEmpty() && headPosition in tape.indices) {
            listState.animateScrollToItem(headPosition)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TAPE_BAR_HEIGHT)
            .clip(MaterialTheme.shapes.medium)
            .border(
                TAPE_BORDER_WIDTH,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.shapes.medium
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = TAPE_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Edit icon
        if (onEditClick != null) {
            Icon(
                painter = painterResource(id = R.drawable.edit_icon),
                contentDescription = "Edit tape",
                modifier = Modifier
                    .size(EDIT_ICON_SIZE)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onEditClick() },
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(TAPE_PADDING))
        }

        // Tape symbols
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(tape) { index, symbol ->
                TapeCell(
                    symbol = symbol,
                    isHead = index == headPosition,
                    headColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(CELL_SPACING))
            }
        }
    }
}
