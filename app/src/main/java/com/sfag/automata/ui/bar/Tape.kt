package com.sfag.automata.ui.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfag.R
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.automata.ui.machine.cellPadding
import com.sfag.automata.ui.machine.cellSize

/**
 * Unified tape bar for all machine types.
 *
 * Head position behavior:
 * - Start of tape : head drifts from left edge toward center (first halfCells items)
 * - Middle of tape : head stays at center (tape scrolls under it)
 * - End of tape : head drifts from center toward right edge (last halfCells items)
 *
 * Both drifts are produced by the natural LazyRow scroll clamping at each end - no explicit
 * start/end padding tricks required. endPadding is kept to just CELL_SPACING so the last cell can
 * reach the right edge at maximum scroll.
 *
 * [listState] must be hoisted outside any key() block in the caller so scroll position survives
 * recomposition-key resets and Tape can animate smoothly between steps.
 */
@Composable
fun Tape(
    symbols: List<Char>,
    headIndex: Int,
    listState: LazyListState,
    onEdit: (() -> Unit)?,
    infiniteRight: Boolean,
    headColor: Color = MaterialTheme.colorScheme.primary,
    isConsumedShown: Boolean = false,
    infiniteLeft: Boolean = false,
    blankChar: Char = ' ',
) {
    val density = LocalDensity.current
    val cellStepPx = remember(density) { with(density) { (cellSize + cellPadding).roundToPx() } }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(cellSize + cellPadding * 2)
                .padding(horizontal = cellPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(cellPadding),
    ) {
        if (onEdit != null) {
            Box(
                modifier =
                    Modifier.size(cellSize)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { onEdit() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.edit_tape),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp,
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val visibleCells = ((maxWidth / (cellSize + cellPadding)).toInt()) + 2
            val rightPad =
                if (infiniteRight) (visibleCells - symbols.size + 1).coerceAtLeast(3) else 0
            val leftPad = if (infiniteLeft) 3 else 0
            val displaySymbols =
                List(leftPad) { blankChar } + symbols + List(rightPad) { blankChar }
            val adjustedHead = headIndex + leftPad

            // Number of cells that fit across half the visible width.
            // scrollOffset = -(halfCells * step) aims for the center of the viewport.
            // Natural clamping creates symmetric left/right drift at tape edges.
            val halfCells = ((maxWidth / (cellSize + cellPadding)).toInt() / 2).coerceAtLeast(1)

            LaunchedEffect(adjustedHead, halfCells) {
                if (displaySymbols.isNotEmpty() && adjustedHead in displaySymbols.indices) {
                    listState.animateScrollToItem(
                        adjustedHead,
                        scrollOffset = -(halfCells * cellStepPx),
                    )
                }
            }

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(end = cellPadding),
                horizontalArrangement = Arrangement.spacedBy(cellPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(displaySymbols) { index, symbol ->
                    Cell(
                        symbol = symbol,
                        isHead = index == adjustedHead,
                        isConsumed = isConsumedShown && index < adjustedHead,
                        headColor = headColor,
                    )
                }
            }
        }
    }
}

@Composable
fun FiniteMachine.Tape(
    listState: LazyListState,
    onEdit: () -> Unit,
    headColor: Color = MaterialTheme.colorScheme.primary,
) {
    val symbols = fullInput
    val headIndex =
        (symbols.length - remainingInput.length).coerceIn(0, (symbols.length - 1).coerceAtLeast(0))

    Tape(
        symbols = symbols.toList(),
        headIndex = headIndex,
        listState = listState,
        headColor = headColor,
        isConsumedShown = true,
        onEdit = onEdit,
        infiniteRight = false,
    )
}

@Composable
fun PushdownMachine.Tape(
    listState: LazyListState,
    onEdit: () -> Unit,
    headColor: Color = MaterialTheme.colorScheme.primary,
) {
    val symbols = fullInput
    val headIndex =
        (symbols.length - remainingInput.length).coerceIn(0, (symbols.length - 1).coerceAtLeast(0))

    Tape(
        symbols = symbols.toList(),
        headIndex = headIndex,
        listState = listState,
        headColor = headColor,
        isConsumedShown = true,
        onEdit = onEdit,
        infiniteRight = false,
    )
}

/** Bidirectional tape for Turing machine simulation. */
@Composable
fun TuringMachine.Tape(
    listState: LazyListState,
    onEdit: () -> Unit,
    headColor: Color = MaterialTheme.colorScheme.primary,
) {
    Tape(
        symbols = tape,
        headIndex = headPosition,
        listState = listState,
        headColor = headColor,
        onEdit = onEdit,
        infiniteRight = true,
        infiniteLeft = true,
    )
}

@Composable
internal fun Cell(
    symbol: Char,
    isHead: Boolean = false,
    isConsumed: Boolean = false,
    headColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = cellSize,
) {
    val bgColor =
        when {
            isHead -> MaterialTheme.colorScheme.surfaceContainerLowest
            isConsumed -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        }
    val textColor =
        when {
            isHead -> headColor
            isConsumed -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        }

    Box(
        modifier =
            Modifier.size(size)
                .clip(MaterialTheme.shapes.small)
                .then(
                    if (isHead) {
                        Modifier.border(2.dp, headColor, MaterialTheme.shapes.small)
                    } else {
                        Modifier
                    }
                )
                .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = textColor,
        )
    }
}
