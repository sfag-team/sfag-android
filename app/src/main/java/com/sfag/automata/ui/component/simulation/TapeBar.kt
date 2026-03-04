package com.sfag.automata.ui.component.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfag.R

/**
 * Unified tape bar used by all machine types.
 *
 * Head position behaviour:
 *  - Start of tape  : head drifts from left edge toward center (first halfCells items)
 *  - Middle of tape : head stays at center (tape scrolls under it)
 *  - End of tape    : head drifts from center toward right edge (last halfCells items)
 *
 * Both drifts are produced by the natural LazyRow scroll clamping at each end - no explicit
 * start/end padding tricks required. endPadding is kept to just CELL_SPACING so the last
 * cell can reach the right edge at maximum scroll.
 *
 * [listState] must be hoisted outside any key() block in the caller so scroll position
 * survives recomposition-key resets and TapeBar can animate smoothly between steps.
 */
@Composable
fun TapeBar(
    symbols: List<Char>,
    headIndex: Int,
    listState: LazyListState = rememberLazyListState(),
    headColor: Color = MaterialTheme.colorScheme.primary,
    showConsumed: Boolean = false,
    onEditClick: (() -> Unit)? = null,
    infiniteRight: Boolean = false,
    infiniteLeft: Boolean = false,
    blankChar: Char = ' '
) {
    val density = LocalDensity.current
    val cellStepPx = remember(density) { with(density) { (CELL_SIZE + CELL_SPACING).roundToPx() } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TAPE_BAR_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (onEditClick != null) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(CELL_SIZE)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { onEditClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.tape_input_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val visibleCells = ((maxWidth / (CELL_SIZE + CELL_SPACING)).toInt()) + 2
            val rightPad = if (infiniteRight) (visibleCells - symbols.size + 1).coerceAtLeast(3) else 0
            val leftPad = if (infiniteLeft) 3 else 0
            val displaySymbols = List(leftPad) { blankChar } + symbols + List(rightPad) { blankChar }
            val adjustedHead = headIndex + leftPad

            // Number of cells that fit across half the visible width.
            // scrollOffset = -(halfCells * step) aims for the center of the viewport.
            // Natural clamping creates symmetric left/right drift at tape edges.
            val halfCells = ((maxWidth / (CELL_SIZE + CELL_SPACING)).toInt() / 2).coerceAtLeast(1)

            LaunchedEffect(adjustedHead, halfCells) {
                if (displaySymbols.isNotEmpty() && adjustedHead in displaySymbols.indices) {
                    listState.animateScrollToItem(adjustedHead, scrollOffset = -(halfCells * cellStepPx))
                }
            }

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(end = CELL_SPACING),
                horizontalArrangement = Arrangement.spacedBy(CELL_SPACING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(displaySymbols) { index, symbol ->
                    TapeCell(
                        symbol = symbol,
                        isHead = index == adjustedHead,
                        isConsumed = showConsumed && index < adjustedHead,
                        headColor = headColor
                    )
                }
            }
        }
        if (onEditClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
