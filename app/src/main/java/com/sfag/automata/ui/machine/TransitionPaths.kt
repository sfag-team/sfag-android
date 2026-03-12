package com.sfag.automata.ui.machine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.Transition
import com.sfag.automata.ui.common.NODE_RADIUS
import com.sfag.automata.ui.common.TRANSITION_CURVATURE
import com.sfag.automata.ui.common.TRANSITION_HEAD_SIZE
import com.sfag.automata.ui.common.TRANSITION_LABEL_OFFSET
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Precomputed path for a single transition's visual representation. */
data class TransitionPath(
    val arrowBody: Path,
    val arrowHead: Path,
    /** For regular: bezier control position. For self-loop: arc peak (outermost position). */
    val controlPoint: Offset,
    /** Text label anchor: near the curve, offset outward from the chord. */
    val textPosition: Offset,
    val isSelfLoop: Boolean,
)

/** Mutable intermediate type for geometry computation before spreading. */
private class MutableGeometry(
    val transitionIndex: Int,
    val transition: Transition,
    var startX: Float,
    var startY: Float,
    var endX: Float,
    var endY: Float,
    var controlX: Float,
    var controlY: Float,
    val nodeCenterX: Float,
    val nodeCenterY: Float,
    var perpX: Float,
    var perpY: Float,
    val isSelfLoop: Boolean = false,
    val loopSign: Float = 0f,
)

/** Converts a dp position (top-left of state box) to screen-pixel center of the node. */
private fun toScreenCenter(
    position: Offset,
    density: Density,
): Offset =
    with(density) {
        Offset(
            (position.x + NODE_RADIUS / 2f).dp.toPx(),
            (position.y + NODE_RADIUS / 2f).dp.toPx(),
        )
    }

/**
 * Computes transition paths for all transitions. Returns a list indexed by transition index;
 * entries are null for transitions where positions are unavailable.
 */
fun Machine.computeTransitionPaths(
    positions: Map<Int, Offset>,
    density: Density,
): List<TransitionPath?> {
    val halfSpread = NODE_RADIUS / sqrt(2f)
    val result = arrayOfNulls<TransitionPath>(transitions.size)

    // Pass 1: build geometry for each transition
    val geometries = mutableListOf<MutableGeometry>()

    for ((index, transition) in transitions.withIndex()) {
        val startPosition = positions[transition.fromState] ?: continue
        val endPosition = positions[transition.toState] ?: continue
        val startCenter = toScreenCenter(startPosition, density)
        val endCenter = toScreenCenter(endPosition, density)

        if (transition.fromState == transition.toState) {
            val connectedStates =
                transitions
                    .mapNotNull { other ->
                        when {
                            other.fromState == transition.fromState &&
                                other.toState != transition.fromState -> other.toState
                            other.toState == transition.fromState &&
                                other.fromState != transition.fromState -> other.fromState
                            else -> null
                        }
                    }.distinct()
            val above =
                connectedStates.count { stateIndex ->
                    (positions[stateIndex]?.y ?: return@count false) < startPosition.y
                }
            val below =
                connectedStates.count { stateIndex ->
                    (positions[stateIndex]?.y ?: return@count false) > startPosition.y
                }
            val loopSign = if (above > below) 1f else -1f

            geometries.add(
                MutableGeometry(
                    transitionIndex = index,
                    transition = transition,
                    startX = startCenter.x + halfSpread,
                    startY = startCenter.y + loopSign * halfSpread,
                    endX = startCenter.x - halfSpread,
                    endY = startCenter.y + loopSign * halfSpread,
                    controlX = startCenter.x,
                    controlY = startCenter.y + loopSign * NODE_RADIUS * 2.5f,
                    nodeCenterX = startCenter.x,
                    nodeCenterY = startCenter.y,
                    perpX = 0f,
                    perpY = loopSign,
                    isSelfLoop = true,
                    loopSign = loopSign,
                ),
            )
        } else {
            val chordDx = endCenter.x - startCenter.x
            val chordDy = endCenter.y - startCenter.y
            val chordLen = sqrt(chordDx * chordDx + chordDy * chordDy)
            if (chordLen < 0.1f) continue

            val chordDirX = chordDx / chordLen
            val chordDirY = chordDy / chordLen
            val midX = (startCenter.x + endCenter.x) / 2f
            val midY = (startCenter.y + endCenter.y) / 2f

            geometries.add(
                MutableGeometry(
                    transitionIndex = index,
                    transition = transition,
                    startX = startCenter.x + NODE_RADIUS * chordDirX,
                    startY = startCenter.y + NODE_RADIUS * chordDirY,
                    endX = endCenter.x - NODE_RADIUS * chordDirX,
                    endY = endCenter.y - NODE_RADIUS * chordDirY,
                    controlX = midX + TRANSITION_CURVATURE * chordDirY * chordLen,
                    controlY = midY + TRANSITION_CURVATURE * -chordDirX * chordLen,
                    nodeCenterX = startCenter.x,
                    nodeCenterY = startCenter.y,
                    perpX = chordDirY,
                    perpY = -chordDirX,
                ),
            )
        }
    }

    // Pass 2: spread connection points at each node to avoid arrowhead overlap
    val regularGeometries = geometries.filter { !it.isSelfLoop }
    for (state in states) {
        val position = positions[state.index] ?: continue
        val nodeCenter = toScreenCenter(position, density)
        spreadConnections(
            regularGeometries.filter { it.transition.toState == state.index },
            nodeCenter,
            isEnd = true,
            minSepDeg = 30f,
        )
        spreadConnections(
            regularGeometries.filter { it.transition.fromState == state.index },
            nodeCenter,
            isEnd = false,
            minSepDeg = 20f,
        )
    }

    // Pass 2.5: recompute control points for regular transitions from the final
    // (potentially spread) connection points, so curves remain smooth after spreading.
    for (geometry in regularGeometries) {
        geometry.recomputeControlPoint()
    }

    // Pass 3: build paths
    for (geometry in geometries) {
        result[geometry.transitionIndex] =
            if (geometry.isSelfLoop) {
                buildSelfLoopPath(geometry, halfSpread)
            } else {
                buildRegularPath(geometry)
            }
    }

    return result.toList()
}

private fun MutableGeometry.recomputeControlPoint() {
    val dx = endX - startX
    val dy = endY - startY
    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    perpX = dy / dist
    perpY = -dx / dist
    val midX = (startX + endX) / 2f
    val midY = (startY + endY) / 2f
    controlX = midX + TRANSITION_CURVATURE * perpX * dist
    controlY = midY + TRANSITION_CURVATURE * perpY * dist
}

private fun buildSelfLoopPath(
    geometry: MutableGeometry,
    halfSpread: Float,
): TransitionPath {
    val arcCenterFactor = 5.25f / (5f - sqrt(2f))
    val arcRadius = (2.5f - arcCenterFactor) * NODE_RADIUS
    val arcCenterY = geometry.nodeCenterY + geometry.loopSign * arcCenterFactor * NODE_RADIUS

    val attachDy = abs(halfSpread - arcCenterFactor * NODE_RADIUS)
    val attachAngle = (atan2(attachDy.toDouble(), halfSpread.toDouble()) * 180.0 / PI).toFloat()
    val startAngle = -geometry.loopSign * attachAngle
    val sweepAngle = geometry.loopSign * (180f + 2f * attachAngle)

    val oval = Rect(
        left = geometry.nodeCenterX - arcRadius,
        top = arcCenterY - arcRadius,
        right = geometry.nodeCenterX + arcRadius,
        bottom = arcCenterY + arcRadius,
    )

    val arrowBody = Path().apply {
        arcTo(oval, startAngle, sweepAngle, forceMoveTo = true)
    }

    val arrowHead = buildArrowHeadFromPath(arrowBody, geometry.endX, geometry.endY)

    return TransitionPath(
        arrowBody = arrowBody,
        arrowHead = arrowHead,
        controlPoint = Offset(geometry.controlX, geometry.controlY),
        textPosition = Offset(
            geometry.controlX,
            geometry.controlY + geometry.loopSign * TRANSITION_LABEL_OFFSET,
        ),
        isSelfLoop = true,
    )
}

private fun buildRegularPath(geometry: MutableGeometry): TransitionPath {
    val arrowBody = Path().apply {
        moveTo(geometry.startX, geometry.startY)
        quadraticTo(
            geometry.controlX, geometry.controlY,
            geometry.endX, geometry.endY,
        )
    }

    val arrowHead = buildArrowHeadFromPath(arrowBody, geometry.endX, geometry.endY)

    val bezierMidX = 0.25f * geometry.startX + 0.5f * geometry.controlX + 0.25f * geometry.endX
    val bezierMidY = 0.25f * geometry.startY + 0.5f * geometry.controlY + 0.25f * geometry.endY

    return TransitionPath(
        arrowBody = arrowBody,
        arrowHead = arrowHead,
        controlPoint = Offset(geometry.controlX, geometry.controlY),
        textPosition = Offset(
            bezierMidX + geometry.perpX * TRANSITION_LABEL_OFFSET,
            bezierMidY + geometry.perpY * TRANSITION_LABEL_OFFSET,
        ),
        isSelfLoop = false,
    )
}

/** Samples the path at HEAD_SIZE back from the end to get direction, then builds equilateral head. */
private fun buildArrowHeadFromPath(path: Path, tipX: Float, tipY: Float): Path {
    val pathMeasure = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val length = pathMeasure.length
    val point = FloatArray(2)
    pathMeasure.getPosTan((length - TRANSITION_HEAD_SIZE).coerceAtLeast(0f), point, null)
    val dx = tipX - point[0]
    val dy = tipY - point[1]
    val dirLen = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    val dirX = dx / dirLen
    val dirY = dy / dirLen
    val baseX = tipX - dirX * TRANSITION_HEAD_SIZE
    val baseY = tipY - dirY * TRANSITION_HEAD_SIZE
    val halfBase = TRANSITION_HEAD_SIZE / sqrt(3f)
    val perpX = -dirY * halfBase
    val perpY = dirX * halfBase
    return Path().apply {
        moveTo(tipX, tipY)
        lineTo(baseX - perpX, baseY - perpY)
        lineTo(baseX + perpX, baseY + perpY)
        close()
    }
}

/**
 * Returns the position along [path] at [progress] in [0, 1]. Uses Android PathMeasure for accuracy
 * on Bézier curves.
 */
internal fun getCurrentPositionByPath(
    path: Path,
    progress: Float,
): Offset {
    val pathMeasure = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val point2D = FloatArray(2)
    pathMeasure.getPosTan(pathMeasure.length * progress.coerceIn(0f, 1f), point2D, null)
    return Offset(point2D[0], point2D[1])
}

private fun spreadConnections(
    geometries: List<MutableGeometry>,
    nodeCenter: Offset,
    isEnd: Boolean,
    minSepDeg: Float,
) {
    val groups =
        geometries.groupBy { if (isEnd) it.transition.fromState else it.transition.toState }
    if (groups.size < 2) return

    val minSep = minSepDeg * PI / 180.0
    val twoPi = 2 * PI
    val groupList = groups.values.toList()

    val rawAngles =
        groupList.map { group ->
            val geometry = group.first()
            val x = if (isEnd) geometry.endX else geometry.startX
            val y = if (isEnd) geometry.endY else geometry.startY
            atan2((y - nodeCenter.y).toDouble(), (x - nodeCenter.x).toDouble())
        }

    val sortedIndices = rawAngles.indices.sortedBy { rawAngles[it] }
    val angles = sortedIndices.map { rawAngles[it] }.toMutableList()
    val sortedGroups = sortedIndices.map { groupList[it] }

    repeat(sortedGroups.size * 3) {
        for (j in angles.indices) {
            val next = (j + 1) % angles.size
            var diff = angles[next] - angles[j]
            if (next == 0) diff += twoPi
            if (diff < minSep) {
                val push = (minSep - diff) / 2.0
                angles[j] -= push
                angles[next] += push
            }
        }
    }

    for ((i, geometryGroup) in sortedGroups.withIndex()) {
        val angle = angles[i]
        val newX = (nodeCenter.x + NODE_RADIUS * cos(angle)).toFloat()
        val newY = (nodeCenter.y + NODE_RADIUS * sin(angle)).toFloat()
        for (geometry in geometryGroup) {
            if (isEnd) {
                geometry.endX = newX
                geometry.endY = newY
            } else {
                geometry.startX = newX
                geometry.startY = newY
            }
        }
    }
}
