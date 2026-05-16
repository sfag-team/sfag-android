package com.sfag.automata.ui.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.sfag.R
import com.sfag.automata.ui.machine.cellPadding
import com.sfag.automata.ui.machine.cellSize

/**
 * Unified tape bar for all machine types. The button sits flush left next to the cells; the cells
 * fill the rest of the row and may be clipped at either edge — the LazyRow scrolls so the head
 * stays centered.
 *
 * Head position behavior:
 * - Start of tape : head drifts from left edge toward center
 * - Middle of tape : head stays at center (tape scrolls under it)
 * - End of tape : head drifts from center toward right edge
 *
 * Both drifts are produced by the natural LazyRow scroll clamping at each end.
 *
 * [listState] must be hoisted outside any key() block in the caller so scroll position survives
 * recomposition-key resets and Tape can animate smoothly between steps.
 */
@Composable
fun Tape(
    symbols: List<Char>,
    headIndex: Int,
    listState: LazyListState,
    onEdit: () -> Unit,
    blankChar: Char = ' ',
    muteHead: Boolean = false,
    muteConsumed: Boolean = false,
    infiniteRight: Boolean = false,
    infiniteLeft: Boolean = false,
) {
    val density = LocalDensity.current

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(cellSize + cellPadding * 2)
                .padding(horizontal = cellPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(cellPadding),
    ) {
        Box(
            modifier =
                Modifier.size(cellSize)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { onEdit() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.input_tape_button),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 14.sp,
            )
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val viewportPx = with(density) { maxWidth.toPx() }
            val cellPx = with(density) { cellSize.toPx() }
            val centerOffset = -((viewportPx - cellPx) / 2).toInt()

            val rightPad = if (infiniteRight) 3 else 0
            val leftPad = if (infiniteLeft) 3 else 0
            val displaySymbols =
                List(leftPad) { blankChar } + symbols + List(rightPad) { blankChar }
            val adjustedHead = headIndex + leftPad

            LaunchedEffect(adjustedHead, centerOffset) {
                if (displaySymbols.isNotEmpty() && adjustedHead in displaySymbols.indices) {
                    listState.animateScrollToItem(adjustedHead, scrollOffset = centerOffset)
                }
            }

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(cellPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(displaySymbols) { index, symbol ->
                    val isHighlighted = index == adjustedHead
                    val isMuted =
                        when {
                            isHighlighted -> muteHead
                            index < adjustedHead -> muteConsumed
                            else -> false
                        }
                    Cell(symbol = symbol, isMuted = isMuted, isHighlighted = isHighlighted)
                }
            }
        }
    }
}
