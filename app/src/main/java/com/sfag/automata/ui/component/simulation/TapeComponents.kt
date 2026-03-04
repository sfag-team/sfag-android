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

// Bar dimensions
val TAPE_BAR_HEIGHT = 56.dp
val MACHINE_DIAGRAM_HEIGHT = 400.dp
val TAPE_PADDING = 4.dp

// Cell dimensions
val CELL_SIZE = 48.dp
val CELL_SPACING = 8.dp

// Icon sizes
val EDIT_ICON_SIZE = 32.dp

@Composable
fun TapeCell(
    symbol: Char,
    isHead: Boolean = false,
    isConsumed: Boolean = false,
    headColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = CELL_SIZE
) {
    val backgroundColor = when {
        isHead -> MaterialTheme.colorScheme.surfaceContainerLowest
        isConsumed -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val textColor = when {
        isHead -> headColor
        isConsumed -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .then(if (isHead) Modifier.border(2.dp, headColor, MaterialTheme.shapes.small) else Modifier)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = textColor
        )
    }
}
