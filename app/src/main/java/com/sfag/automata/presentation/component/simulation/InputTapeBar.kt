package com.sfag.automata.presentation.component.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfag.R
import com.sfag.automata.domain.model.machine.Machine

/**
 * Input tape bar for Finite Automata and Pushdown Automata.
 *
 * Displays the input string at the TOP of the simulation view.
 * The head moves left-to-right as symbols are consumed.
 * Includes an edit icon to modify the input.
 *
 * Position: TOP
 * Used by: FA, PDA
 */
@Composable
fun Machine.InputTapeBar(onEditClick: () -> Unit) {
    val fullWord = remember(fullInputSnapshot, input.toString()) {
        if (fullInputSnapshot.isNotEmpty()) fullInputSnapshot else input.toString()
    }
    val headIndex = remember(fullWord, imuInput.toString()) {
        val consumed = (fullWord.length - imuInput.length).coerceIn(0, fullWord.length)
        consumed.coerceAtMost((fullWord.length - 1).coerceAtLeast(0))
    }

    InputTapeBar(
        symbols = fullWord.toList(),
        headIndex = headIndex,
        onEditClick = onEditClick
    )
}

/**
 * Input tape bar with explicit parameters.
 * Decoupled from Machine for reusability and testing.
 */
@Composable
fun InputTapeBar(
    symbols: List<Char>,
    headIndex: Int,
    onEditClick: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()

    LaunchedEffect(headIndex) {
        if (symbols.isNotEmpty() && headIndex in symbols.indices) {
            listState.animateScrollToItem(headIndex)
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
                contentDescription = "Edit input",
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
            itemsIndexed(symbols) { index, symbol ->
                TapeCell(
                    symbol = symbol,
                    isHead = index == headIndex,
                    headColor = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(CELL_SPACING))
            }
        }
    }
}
