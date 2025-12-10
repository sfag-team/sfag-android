package com.sfag.automata.ui.component.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared constants for tape/bar components to ensure visual consistency.
 */

// Bar dimensions
val TAPE_BAR_HEIGHT = 60.dp
val TAPE_PADDING = 10.dp
val TAPE_BORDER_WIDTH = 3.dp

// Cell dimensions
val CELL_SIZE = 46.dp
val CELL_SPACING = 8.dp
val CELL_BORDER_WIDTH = 2.dp
val CELL_BORDER_WIDTH_HEAD = 4.dp

// Icon sizes
val EDIT_ICON_SIZE = 28.dp

// Font
val CELL_FONT_SIZE = 26.sp

/**
 * A single cell on a tape (input tape, Turing tape, or stack).
 *
 * @param symbol The character to display
 * @param isHead Whether this cell is at the current head position
 * @param headColor Color to use when this is the head position
 * @param size Size of the cell (default: CELL_SIZE)
 */
@Composable
fun TapeCell(
    symbol: Char,
    isHead: Boolean = false,
    headColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = CELL_SIZE
) {
    val borderWidth = if (isHead) CELL_BORDER_WIDTH_HEAD else CELL_BORDER_WIDTH
    val borderColor = if (isHead) headColor else MaterialTheme.colorScheme.tertiary
    val backgroundColor = if (isHead) {
        headColor.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.background
    }
    val textColor = if (isHead) headColor else MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .border(borderWidth, borderColor, MaterialTheme.shapes.medium)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol.toString(),
            fontSize = CELL_FONT_SIZE,
            color = textColor
        )
    }
}

/**
 * A stack cell - similar to TapeCell but without head highlighting.
 * Used for PDA stack display.
 */
@Composable
fun StackCell(
    symbol: Char,
    size: Dp = CELL_SIZE
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .border(CELL_BORDER_WIDTH, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol.toString(),
            fontSize = CELL_FONT_SIZE,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}
