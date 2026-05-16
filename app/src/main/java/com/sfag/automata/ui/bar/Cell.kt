package com.sfag.automata.ui.bar

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sfag.automata.ui.machine.cellSize

/** Reusable single-symbol cell shared by Tape and Stack. */
@Composable
internal fun Cell(
    symbol: Char,
    isMuted: Boolean = false,
    isHighlighted: Boolean = false,
    size: Dp = cellSize,
) {
    val borderColor =
        when {
            isHighlighted && isMuted -> MaterialTheme.colorScheme.onSurfaceVariant
            isHighlighted -> MaterialTheme.colorScheme.primary
            else -> null
        }
    val bgColor =
        if (isMuted) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainerLowest
    val textColor =
        when {
            isMuted -> MaterialTheme.colorScheme.onSurfaceVariant
            isHighlighted -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }

    Box(
        modifier =
            Modifier.size(size)
                .clip(MaterialTheme.shapes.small)
                .then(
                    if (borderColor != null) {
                        Modifier.border(2.dp, borderColor, MaterialTheme.shapes.small)
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
