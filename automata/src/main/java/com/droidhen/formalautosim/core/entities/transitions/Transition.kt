package com.droidhen.formalautosim.core.entities.transitions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import com.droidhen.formalautosim.core.entities.states.State
import kotlin.math.*

class Transition(var name: String = "a", val startState: Int, val endState: Int) {

    var controlPoint: Offset? = null

    fun isClicked(x:Dp, y:Dp, getStateByIndex: (Int) -> State?): Boolean {
        if (controlPoint==null) return false
        val start = getStateByIndex(startState) ?: return false
        val end = getStateByIndex(endState) ?: return false

        val startCenter = start.position
        val endCenter = end.position

        val startRadius = start.radius
        val endRadius = end.radius

        val dx = endCenter.x - startCenter.x
        val dy = endCenter.y - startCenter.y
        val length = sqrt(dx * dx + dy * dy)

        if (length == 0f) return false

        val dirX = dx / length
        val dirY = dy / length

        val startPoint = Offset(startCenter.x + dirX * startRadius, startCenter.y + dirY * startRadius)
        val endPoint = Offset(endCenter.x - dirX * endRadius, endCenter.y - dirY * endRadius)

        val hitBoxWidth = 20f

        val normalX = -dirY * hitBoxWidth
        val normalY = dirX * hitBoxWidth

        val p1 = Offset(startPoint.x + normalX, startPoint.y + normalY)
        val p2 = Offset(startPoint.x - normalX, startPoint.y - normalY)
        val p3 = Offset(endPoint.x + normalX, endPoint.y + normalY)
        val p4 = Offset(endPoint.x - normalX, endPoint.y - normalY)

        val controlP1 = Offset(controlPoint!!.x + normalX, controlPoint!!.y + normalY)
        val controlP2 = Offset(controlPoint!!.x - normalX, controlPoint!!.y - normalY)

        return isPointInsidePolygon(
            Offset(x.value, y.value),
            listOf(p1, controlP1, p3, p4, controlP2, p2)
        )
    }


    private fun isPointInsidePolygon(point: Offset, polygon: List<Offset>): Boolean {
        var intersections = 0
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[(i + 1) % polygon.size]

            if ((a.y > point.y) != (b.y > point.y) &&
                (point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x)
            ) {
                intersections++
            }
        }
        return intersections % 2 == 1
    }

}