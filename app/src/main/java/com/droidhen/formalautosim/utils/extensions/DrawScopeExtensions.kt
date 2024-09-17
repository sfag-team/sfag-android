package com.droidhen.formalautosim.utils.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * draws arrow
 *
 * @param arrow - Pair Path (arrow) to Path (arrow head)
 * @param color - color of arrow (default - Black)
 */
fun DrawScope.drawArrow(
    arrow:Pair<Path?, Path?>,
    color: Color = Color.Black
) {
    if(arrow.first==null||arrow.second==null) return
    drawPath(arrow.first!!,color = color, style = Stroke(
        width = 3f,
        cap = StrokeCap.Round
    ))
    drawPath(arrow.second!!, color
    )
}