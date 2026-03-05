package com.sfag.automata.ui.diagram

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

import com.sfag.automata.NODE_OUTLINE_WIDTH
import com.sfag.automata.NODE_RADIUS
import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.transition.Transition

private const val CURVATURE = 0.125f
private const val HEAD_LENGTH = 26f
private const val HEAD_HALF = HEAD_LENGTH * 0.55f

/** Outer edge radius of the node circle (connection point radius), in screen px. */
private const val CONNECTION_RADIUS = NODE_RADIUS + NODE_OUTLINE_WIDTH / 2f

private data class TransitionGeom(
    val transitionIdx: Int,
    val transition: Transition,
    var startOffset: Offset,
    var endOffset: Offset,
    var controlPoint: Offset,
    val startCenter: Offset,
    val endCenter: Offset,
    /** Perpendicular unit vector (outward curvature direction). */
    var perpX: Float,
    var perpY: Float,
    // Self-loop only
    val isSelfLoop: Boolean = false,
    val loopSign: Float = 0f,
    val peakY: Float = 0f
)

/** Converts a dp position (top-left of state box) to screen-pixel center of the node. */
private fun toScreenCenter(pos: Offset, density: Density): Offset = with(density) {
    Offset((pos.x + NODE_RADIUS / 2f).dp.toPx(), (pos.y + NODE_RADIUS / 2f).dp.toPx())
}

/** Returns the unit vector from [from] toward [to]. */
private fun normalizedDir(from: Offset, to: Offset): Pair<Float, Float> {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val inv = 1f / sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    return dx * inv to dy * inv
}

private fun getArrowHeadPath(tip: Offset, dirX: Float, dirY: Float): Path {
    val perpX = -dirY * HEAD_HALF
    val perpY = dirX * HEAD_HALF
    val baseX = tip.x - dirX * HEAD_LENGTH
    val baseY = tip.y - dirY * HEAD_LENGTH
    return Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(baseX - perpX, baseY - perpY)
        lineTo(baseX + perpX, baseY + perpY)
        close()
    }
}

/**
 * Computes render paths for all transitions.
 * Returns a list indexed by transition index; entries are null for transitions
 * where positions are unavailable.
 */
fun Machine.computePaths(
    positions: Map<Int, Offset>,
    density: Density
): List<TransitionRenderer?> {
    val r = CONNECTION_RADIUS
    val halfSpread = r / sqrt(2f)
    val result = arrayOfNulls<TransitionRenderer>(transitions.size)

    // Pass 1: build geom for each transition
    val geoms = mutableListOf<TransitionGeom>()

    for ((idx, transition) in transitions.withIndex()) {
        val startPos = positions[transition.startState] ?: continue
        val endPos = positions[transition.endState] ?: continue
        val startCenter = toScreenCenter(startPos, density)
        val endCenter = toScreenCenter(endPos, density)

        if (transition.startState == transition.endState) {
            // Self-loop: determine above/below based on unique connected states
            val connectedStates = transitions.mapNotNull { t ->
                when {
                    t.startState == transition.startState && t.endState != transition.startState -> t.endState
                    t.endState == transition.startState && t.startState != transition.startState -> t.startState
                    else -> null
                }
            }.distinct()
            val above = connectedStates.count { idx ->
                (positions[idx]?.y ?: return@count false) < startPos.y
            }
            val below = connectedStates.count { idx ->
                (positions[idx]?.y ?: return@count false) > startPos.y
            }
            // More connections at top half → loop below; otherwise loop above
            val loopSign = if (above > below) 1f else -1f
            val peakY = startCenter.y + loopSign * r * 2.5f
            val rightAttach = Offset(startCenter.x + halfSpread, startCenter.y + loopSign * halfSpread)
            val leftAttach = Offset(startCenter.x - halfSpread, startCenter.y + loopSign * halfSpread)

            geoms.add(
                TransitionGeom(
                    transitionIdx = idx,
                    transition = transition,
                    startOffset = rightAttach,
                    endOffset = leftAttach,
                    controlPoint = Offset(startCenter.x, peakY),
                    startCenter = startCenter,
                    endCenter = startCenter,
                    perpX = 0f, perpY = loopSign,
                    isSelfLoop = true, loopSign = loopSign, peakY = peakY
                )
            )
        } else {
            // Regular transition
            val chordDx = endCenter.x - startCenter.x
            val chordDy = endCenter.y - startCenter.y
            val chordLen = sqrt(chordDx * chordDx + chordDy * chordDy)
            if (chordLen < 0.1f) continue

            val chordDirX = chordDx / chordLen
            val chordDirY = chordDy / chordLen

            val midX = (startCenter.x + endCenter.x) / 2f
            val midY = (startCenter.y + endCenter.y) / 2f
            val controlPoint = Offset(
                midX + CURVATURE * chordDirY * chordLen,
                midY + CURVATURE * -chordDirX * chordLen
            )

            // Place connection points on the chord (center-to-center line) so that
            // transitions from one node to different targets start at angles reflecting
            // their actual geometric relationship. spreadConnections adjusts if needed,
            // and Pass 2.5 recomputes smooth curves from the final positions.
            val startOffset = Offset(startCenter.x + r * chordDirX, startCenter.y + r * chordDirY)
            val endOffset = Offset(endCenter.x - r * chordDirX, endCenter.y - r * chordDirY)

            geoms.add(
                TransitionGeom(
                    transitionIdx = idx,
                    transition = transition,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    controlPoint = controlPoint,
                    startCenter = startCenter,
                    endCenter = endCenter,
                    perpX = chordDirY, perpY = -chordDirX
                )
            )
        }
    }

    // Pass 2: spread connection points at each node to avoid arrowhead overlap
    for (state in states) {
        val nodeCenter = positions[state.index]?.let { toScreenCenter(it, density) } ?: continue
        val nodeGeoms = geoms.filter { !it.isSelfLoop }
        spreadConnections(nodeGeoms.filter { it.transition.endState == state.index }, nodeCenter, isEnd = true, minSepDeg = 30f)
        spreadConnections(nodeGeoms.filter { it.transition.startState == state.index }, nodeCenter, isEnd = false, minSepDeg = 20f)
    }

    // Pass 2.5: recompute control points for regular transitions from the final
    // (potentially spread) connection points, so curves remain smooth after spreading.
    // Always use the counterclockwise perpendicular of the start→end segment direction.
    // This ensures consistent curvature: curves bow "left" when looking from start to end,
    // so A→B and B→A naturally bow on opposite sides without sign-flip logic.
    for (geom in geoms) {
        if (geom.isSelfLoop) continue
        val dx = geom.endOffset.x - geom.startOffset.x
        val dy = geom.endOffset.y - geom.startOffset.y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        val newPerpX = dy / dist
        val newPerpY = -dx / dist
        val midX = (geom.startOffset.x + geom.endOffset.x) / 2f
        val midY = (geom.startOffset.y + geom.endOffset.y) / 2f
        geom.controlPoint = Offset(
            midX + CURVATURE * newPerpX * dist,
            midY + CURVATURE * newPerpY * dist
        )
        geom.perpX = newPerpX
        geom.perpY = newPerpY
    }

    // Pass 3: build paths from finalised geoms
    for (geom in geoms) {
        val idx = geom.transitionIdx
        if (geom.isSelfLoop) {
            // True circular arc: compute the unique circle through rightAttach, leftAttach, peak.
            // Derived: arcCenter.y = nodeCy + loopSign * arcCenterFactor * r
            val arcCenterFactor = 5.25f / (5f - sqrt(2f))   // ≈ 1.464
            val arcRadius = (2.5f - arcCenterFactor) * r     // ≈ 1.036 * r
            val arcCenterY = geom.startCenter.y + geom.loopSign * arcCenterFactor * r

            // Angle of the rightAttach point relative to the arc center
            val attachDy = abs(halfSpread - arcCenterFactor * r)  // always positive
            val attachAngle = (atan2(attachDy.toDouble(), halfSpread.toDouble()) * 180.0 / PI).toFloat()
            val startAngle = -geom.loopSign * attachAngle           // +47° above / -47° below
            val sweepAngle = geom.loopSign * (180f + 2f * attachAngle)  // ±274° long arc

            val oval = Rect(
                left = geom.startCenter.x - arcRadius,
                top = arcCenterY - arcRadius,
                right = geom.startCenter.x + arcRadius,
                bottom = arcCenterY + arcRadius
            )
            val bodyPath = Path().apply {
                arcTo(oval, startAngle, sweepAngle, forceMoveTo = true)
            }

            // Exact arc tangent at endpoint: (-sin(endAngle), cos(endAngle)) * sign(sweep)
            val endAngleRad = (startAngle + sweepAngle) * PI / 180.0
            val sweepSign = if (sweepAngle > 0) 1f else -1f
            val headDirX = (-sin(endAngleRad) * sweepSign).toFloat()
            val headDirY = (cos(endAngleRad) * sweepSign).toFloat()
            val headPath = getArrowHeadPath(geom.endOffset, headDirX, headDirY)
            val textPosition = Offset(
                geom.controlPoint.x,
                geom.controlPoint.y + geom.loopSign * NODE_RADIUS * 2f
            )
            result[idx] = TransitionRenderer(
                bodyPath = bodyPath,
                headPath = headPath,
                controlPoint = geom.controlPoint,
                textPosition = textPosition,
                startConnectionOffset = geom.startOffset,
                endConnectionOffset = geom.endOffset,
                isSelfLoop = true
            )
        } else {
            val bodyPath = Path().apply {
                moveTo(geom.startOffset.x, geom.startOffset.y)
                quadraticTo(geom.controlPoint.x, geom.controlPoint.y, geom.endOffset.x, geom.endOffset.y)
            }
            // Arrowhead follows bezier tangent at t=1: direction from control point to end
            val (headDirX, headDirY) = normalizedDir(geom.controlPoint, geom.endOffset)
            val headPath = getArrowHeadPath(geom.endOffset, headDirX, headDirY)

            // Text at bezier midpoint (t=0.5) offset outward by 2*NODE_RADIUS
            val bezierMidX = 0.25f * geom.startOffset.x + 0.5f * geom.controlPoint.x + 0.25f * geom.endOffset.x
            val bezierMidY = 0.25f * geom.startOffset.y + 0.5f * geom.controlPoint.y + 0.25f * geom.endOffset.y
            val textPosition = Offset(
                bezierMidX + geom.perpX * NODE_RADIUS * 2f,
                bezierMidY + geom.perpY * NODE_RADIUS * 2f
            )
            result[idx] = TransitionRenderer(
                bodyPath = bodyPath,
                headPath = headPath,
                controlPoint = geom.controlPoint,
                textPosition = textPosition,
                startConnectionOffset = geom.startOffset,
                endConnectionOffset = geom.endOffset,
                isSelfLoop = false
            )
        }
    }

    return result.toList()
}

private fun spreadConnections(
    geoms: List<TransitionGeom>,
    nodeCenter: Offset,
    isEnd: Boolean,
    minSepDeg: Float
) {
    val groups = geoms.groupBy { if (isEnd) it.transition.startState else it.transition.endState }
    if (groups.size < 2) return

    val minSep = minSepDeg * PI / 180.0
    val twoPi = 2 * PI
    val groupList = groups.values.toList()

    val rawAngles = groupList.map { g ->
        val pt = if (isEnd) g.first().endOffset else g.first().startOffset
        atan2((pt.y - nodeCenter.y).toDouble(), (pt.x - nodeCenter.x).toDouble())
    }

    // Sort by angle so the cyclic iterative push works correctly
    val sortedIndices = rawAngles.indices.sortedBy { rawAngles[it] }
    val angles = sortedIndices.map { rawAngles[it] }.toMutableList()
    val sortedGroups = sortedIndices.map { groupList[it] }

    // Iterative push (cyclic)
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

    // Write back adjusted connection offsets
    for ((i, group) in sortedGroups.withIndex()) {
        val angle = angles[i]
        val newOffset = Offset(
            (nodeCenter.x + CONNECTION_RADIUS * cos(angle)).toFloat(),
            (nodeCenter.y + CONNECTION_RADIUS * sin(angle)).toFloat()
        )
        for (geom in group) {
            if (isEnd) geom.endOffset = newOffset else geom.startOffset = newOffset
        }
    }
}

/**
 * Returns the position along [path] at [progress] ∈ [0, 1].
 * Uses Android PathMeasure for accuracy on bezier curves.
 */
fun getCurrentPositionByPath(path: Path, progress: Float): Offset {
    val pm = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val coords = FloatArray(2)
    pm.getPosTan(pm.length * progress.coerceIn(0f, 1f), coords, null)
    return Offset(coords[0], coords[1])
}
