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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val ARROW_CURVATURE = 0.125f
private const val ARROW_HEAD_LENGTH = NODE_RADIUS * 0.5f
private const val ARROW_HEAD_HALF_WIDTH = ARROW_HEAD_LENGTH * 0.55f

/** Precomputed path for a single transition's visual representation. */
data class ArrowPath(
    val bodyPath: Path,
    val headPath: Path,
    /** For regular: bezier control position. For self-loop: arc peak (outermost position). */
    val controlPoint: Offset,
    /** Text label anchor: near the curve, offset outward from the chord. */
    val textPosition: Offset,
    val isSelfLoop: Boolean,
)

/** Intermediate geometry used during path computation. */
private data class ArrowGeometry(
    val transitionIndex: Int,
    val transition: Transition,
    var startOffset: Offset,
    var endOffset: Offset,
    var controlPoint: Offset,
    val startCenter: Offset,
    val endCenter: Offset,
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
        Offset((position.x + NODE_RADIUS / 2f).dp.toPx(), (position.y + NODE_RADIUS / 2f).dp.toPx())
    }

/** Returns the unit vector from [from] toward [to]. */
private fun normalizedDir(
    from: Offset,
    to: Offset,
): Pair<Float, Float> {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val inv = 1f / sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    return dx * inv to dy * inv
}

private fun getArrowHeadGeometry(
    tip: Offset,
    dirX: Float,
    dirY: Float,
): Path {
    val perpX = -dirY * ARROW_HEAD_HALF_WIDTH
    val perpY = dirX * ARROW_HEAD_HALF_WIDTH
    val baseX = tip.x - dirX * ARROW_HEAD_LENGTH
    val baseY = tip.y - dirY * ARROW_HEAD_LENGTH
    return Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(baseX - perpX, baseY - perpY)
        lineTo(baseX + perpX, baseY + perpY)
        close()
    }
}

/**
 * Computes render paths for all transitions. Returns a list indexed by transition index; entries
 * are null for transitions where positions are unavailable.
 */
fun Machine.computePaths(
    positions: Map<Int, Offset>,
    density: Density,
): List<ArrowPath?> {
    val halfSpread = NODE_RADIUS * 1.125f / sqrt(2f)
    val result = arrayOfNulls<ArrowPath>(transitions.size)

    // Pass 1: build geom for each transition
    val geometries = mutableListOf<ArrowGeometry>()

    for ((index, transition) in transitions.withIndex()) {
        val startPos = positions[transition.fromState] ?: continue
        val endPos = positions[transition.toState] ?: continue
        val startCenter = toScreenCenter(startPos, density)
        val endCenter = toScreenCenter(endPos, density)

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
                    (positions[stateIndex]?.y ?: return@count false) < startPos.y
                }
            val below =
                connectedStates.count { stateIndex ->
                    (positions[stateIndex]?.y ?: return@count false) > startPos.y
                }
            val loopSign = if (above > below) 1f else -1f
            val peakY = startCenter.y + loopSign * NODE_RADIUS * 1.125f * 2.5f
            val rightAttach =
                Offset(startCenter.x + halfSpread, startCenter.y + loopSign * halfSpread)
            val leftAttach =
                Offset(startCenter.x - halfSpread, startCenter.y + loopSign * halfSpread)

            geometries.add(
                ArrowGeometry(
                    transitionIndex = index,
                    transition = transition,
                    startOffset = rightAttach,
                    endOffset = leftAttach,
                    controlPoint = Offset(startCenter.x, peakY),
                    startCenter = startCenter,
                    endCenter = startCenter,
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
            val controlPoint =
                Offset(
                    midX + ARROW_CURVATURE * chordDirY * chordLen,
                    midY + ARROW_CURVATURE * -chordDirX * chordLen,
                )

            val startOffset =
                Offset(startCenter.x + NODE_RADIUS * 1.125f * chordDirX, startCenter.y + NODE_RADIUS * 1.125f * chordDirY)
            val endOffset =
                Offset(endCenter.x - NODE_RADIUS * 1.125f * chordDirX, endCenter.y - NODE_RADIUS * 1.125f * chordDirY)

            geometries.add(
                ArrowGeometry(
                    transitionIndex = index,
                    transition = transition,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    controlPoint = controlPoint,
                    startCenter = startCenter,
                    endCenter = endCenter,
                    perpX = chordDirY,
                    perpY = -chordDirX,
                ),
            )
        }
    }

    // Pass 2: spread connection points at each node to avoid arrowhead overlap
    for (state in states) {
        val nodeCenter = positions[state.index]?.let { toScreenCenter(it, density) } ?: continue
        val nonLoopGeometries = geometries.filter { !it.isSelfLoop }
        spreadConnections(
            nonLoopGeometries.filter { it.transition.toState == state.index },
            nodeCenter,
            isEnd = true,
            minSepDeg = 30f,
        )
        spreadConnections(
            nonLoopGeometries.filter { it.transition.fromState == state.index },
            nodeCenter,
            isEnd = false,
            minSepDeg = 20f,
        )
    }

    // Pass 2.5: recompute control points for regular transitions from the final
    // (potentially spread) connection points, so curves remain smooth after spreading.
    for (geometry in geometries) {
        if (geometry.isSelfLoop) continue
        geometry.recomputeControlPoint()
    }

    // Pass 3: build paths from finalized geometries
    for (geometry in geometries) {
        val index = geometry.transitionIndex
        result[index] =
            if (geometry.isSelfLoop) {
                buildSelfLoopPath(geometry, halfSpread)
            } else {
                buildRegularPath(geometry)
            }
    }

    return result.toList()
}

private fun ArrowGeometry.recomputeControlPoint() {
    val dx = endOffset.x - startOffset.x
    val dy = endOffset.y - startOffset.y
    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    val newPerpX = dy / dist
    val newPerpY = -dx / dist
    val midX = (startOffset.x + endOffset.x) / 2f
    val midY = (startOffset.y + endOffset.y) / 2f
    controlPoint = Offset(midX + ARROW_CURVATURE * newPerpX * dist, midY + ARROW_CURVATURE * newPerpY * dist)
    perpX = newPerpX
    perpY = newPerpY
}

private fun buildSelfLoopPath(
    geometry: ArrowGeometry,
    halfSpread: Float,
): ArrowPath {
    val arcCenterFactor = 5.25f / (5f - sqrt(2f))
    val arcRadius = (2.5f - arcCenterFactor) * NODE_RADIUS * 1.125f
    val arcCenterY = geometry.startCenter.y + geometry.loopSign * arcCenterFactor * NODE_RADIUS * 1.125f

    val attachDy = abs(halfSpread - arcCenterFactor * NODE_RADIUS * 1.125f)
    val attachAngle = (atan2(attachDy.toDouble(), halfSpread.toDouble()) * 180.0 / PI).toFloat()
    val startAngle = -geometry.loopSign * attachAngle
    val sweepAngle = geometry.loopSign * (180f + 2f * attachAngle)

    val oval =
        Rect(
            left = geometry.startCenter.x - arcRadius,
            top = arcCenterY - arcRadius,
            right = geometry.startCenter.x + arcRadius,
            bottom = arcCenterY + arcRadius,
        )
    val endAngleRad = (startAngle + sweepAngle) * PI / 180.0
    val sweepSign = if (sweepAngle > 0) 1f else -1f
    val headDirX = (-sin(endAngleRad) * sweepSign).toFloat()
    val headDirY = (cos(endAngleRad) * sweepSign).toFloat()
    val tipOffset = Offset(geometry.endOffset.x, geometry.endOffset.y)
    val bodyPath =
        Path().apply {
            arcTo(oval, startAngle, sweepAngle, forceMoveTo = true)
            lineTo(tipOffset.x, tipOffset.y)
        }
    val headPath = getArrowHeadGeometry(tipOffset, headDirX, headDirY)
    val textPosition =
        Offset(
            geometry.controlPoint.x,
            geometry.controlPoint.y + geometry.loopSign * NODE_RADIUS * 2f,
        )
    return ArrowPath(
        bodyPath = bodyPath,
        headPath = headPath,
        controlPoint = geometry.controlPoint,
        textPosition = textPosition,
        isSelfLoop = true,
    )
}

private fun buildRegularPath(geometry: ArrowGeometry): ArrowPath {
    val bodyPath =
        Path().apply {
            moveTo(geometry.startOffset.x, geometry.startOffset.y)
            quadraticTo(
                geometry.controlPoint.x,
                geometry.controlPoint.y,
                geometry.endOffset.x,
                geometry.endOffset.y,
            )
        }
    val (headDirX, headDirY) = normalizedDir(geometry.controlPoint, geometry.endOffset)
    val headPath = getArrowHeadGeometry(geometry.endOffset, headDirX, headDirY)

    val bezierMidX =
        0.25f * geometry.startOffset.x +
            0.5f * geometry.controlPoint.x +
            0.25f * geometry.endOffset.x
    val bezierMidY =
        0.25f * geometry.startOffset.y +
            0.5f * geometry.controlPoint.y +
            0.25f * geometry.endOffset.y
    val textPosition =
        Offset(
            bezierMidX + geometry.perpX * NODE_RADIUS * 2f,
            bezierMidY + geometry.perpY * NODE_RADIUS * 2f,
        )
    return ArrowPath(
        bodyPath = bodyPath,
        headPath = headPath,
        controlPoint = geometry.controlPoint,
        textPosition = textPosition,
        isSelfLoop = false,
    )
}

private fun spreadConnections(
    geometries: List<ArrowGeometry>,
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
            val offset = if (isEnd) group.first().endOffset else group.first().startOffset
            atan2((offset.y - nodeCenter.y).toDouble(), (offset.x - nodeCenter.x).toDouble())
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
        val newOffset =
            Offset(
                (nodeCenter.x + NODE_RADIUS * 1.125f * cos(angle)).toFloat(),
                (nodeCenter.y + NODE_RADIUS * 1.125f * sin(angle)).toFloat(),
            )
        for (geometry in geometryGroup) {
            if (isEnd) geometry.endOffset = newOffset else geometry.startOffset = newOffset
        }
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
