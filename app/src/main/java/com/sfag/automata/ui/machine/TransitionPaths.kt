package com.sfag.automata.ui.machine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.Transition
import com.sfag.automata.ui.common.NODE_RADIUS
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val PATH_CURVATURE = 0.125f
private const val LABEL_OFFSET = NODE_RADIUS * 1.75f

// Precomputed geometry constants (avoids runtime sqrt on constant values)
private const val LOOP_ATTACH = NODE_RADIUS * 0.7071068f // NODE_RADIUS / sqrt(2)
private const val LOOP_CENTER = 1.4641016f // 5.25 / (5 - sqrt(2))

/** Precomputed path for a single transition's visual representation. */
data class TransitionPath(
    val arrowBody: Path,
    val tipX: Float,
    val tipY: Float,
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
    pxPerDp: Float,
): Offset =
    Offset(
        (position.x + NODE_RADIUS / 2f) * pxPerDp,
        (position.y + NODE_RADIUS / 2f) * pxPerDp,
    )

/**
 * Computes transition paths for all transitions. Returns a list indexed by transition index;
 * entries are null for transitions where positions are unavailable.
 */
fun Machine.computeTransitionPaths(
    positions: Map<Int, Offset>,
    pxPerDp: Float,
): List<TransitionPath?> {
    val result = arrayOfNulls<TransitionPath>(transitions.size)

    // Pass 1: build geometry for each transition
    val geometries = mutableListOf<MutableGeometry>()

    for ((index, transition) in transitions.withIndex()) {
        val startPosition = positions[transition.fromState] ?: continue
        val endPosition = positions[transition.toState] ?: continue
        val startCenter = toScreenCenter(startPosition, pxPerDp)
        val endCenter = toScreenCenter(endPosition, pxPerDp)

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
                    startX = startCenter.x + LOOP_ATTACH,
                    startY = startCenter.y + loopSign * LOOP_ATTACH,
                    endX = startCenter.x - LOOP_ATTACH,
                    endY = startCenter.y + loopSign * LOOP_ATTACH,
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
                    controlX = midX + PATH_CURVATURE * chordDirY * chordLen,
                    controlY = midY + PATH_CURVATURE * -chordDirX * chordLen,
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
        val nodeCenter = toScreenCenter(position, pxPerDp)
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
                buildSelfLoopPath(geometry)
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
    controlX = midX + PATH_CURVATURE * perpX * dist
    controlY = midY + PATH_CURVATURE * perpY * dist
}

private fun buildSelfLoopPath(geometry: MutableGeometry): TransitionPath {
    val arcRadius = (2.5f - LOOP_CENTER) * NODE_RADIUS
    val arcCenterY = geometry.nodeCenterY + geometry.loopSign * LOOP_CENTER * NODE_RADIUS

    val attachDy = abs(LOOP_ATTACH - LOOP_CENTER * NODE_RADIUS)
    val attachAngle = (atan2(attachDy.toDouble(), LOOP_ATTACH.toDouble()) * 180.0 / PI).toFloat()
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

    return TransitionPath(
        arrowBody = arrowBody,
        tipX = geometry.endX,
        tipY = geometry.endY,
        controlPoint = Offset(geometry.controlX, geometry.controlY),
        textPosition = Offset(
            geometry.controlX,
            geometry.controlY + geometry.loopSign * LABEL_OFFSET,
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

    val bezierMidX = 0.25f * geometry.startX + 0.5f * geometry.controlX + 0.25f * geometry.endX
    val bezierMidY = 0.25f * geometry.startY + 0.5f * geometry.controlY + 0.25f * geometry.endY

    return TransitionPath(
        arrowBody = arrowBody,
        tipX = geometry.endX,
        tipY = geometry.endY,
        controlPoint = Offset(geometry.controlX, geometry.controlY),
        textPosition = Offset(
            bezierMidX + geometry.perpX * LABEL_OFFSET,
            bezierMidY + geometry.perpY * LABEL_OFFSET,
        ),
        isSelfLoop = false,
    )
}

/** Checks if (tapX, tapY) is within [threshold] pixels of any point on [path]. */
internal fun isPathHit(path: Path, tapX: Float, tapY: Float, threshold: Float): Boolean {
    val thresholdSq = threshold * threshold
    val pathMeasure = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val length = pathMeasure.length
    val point = FloatArray(2)
    var distance = 0f
    while (distance <= length) {
        pathMeasure.getPosTan(distance, point, null)
        val dx = tapX - point[0]
        val dy = tapY - point[1]
        if (dx * dx + dy * dy < thresholdSq) return true
        distance += threshold
    }
    pathMeasure.getPosTan(length, point, null)
    val dx = tapX - point[0]
    val dy = tapY - point[1]
    return dx * dx + dy * dy < thresholdSq
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
