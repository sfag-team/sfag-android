package com.sfag.automata.presentation.extension

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import com.sfag.automata.presentation.component.SIM_EDGE_LINE_WIDTH

/**
 * draws arrow
 *
 * @param arrow - Pair Path (arrow) to Path (arrow head)
 * @param color - color of arrow (default - Black)
 */
fun DrawScope.drawArrow(
    arrow:Pair<Path?, Path?>,
    color: Color = Color.Black,
) {
    val arrowPath = arrow.first ?: return
    val arrowHead = arrow.second ?: return
    drawPath(arrowPath, color = color, style = Stroke(
        width = SIM_EDGE_LINE_WIDTH,
        cap = StrokeCap.Round
    ))
    drawPath(arrowHead, color)
}
