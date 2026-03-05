package com.sfag.automata.ui.diagram

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

import com.sfag.automata.NODE_RADIUS
import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.simulation.TransitionData

/**
 * Animates one or more transitions simultaneously during a simulation step.
 * All circles share a single progress value so they move in lockstep.
 */
@Composable
fun Machine.MultipleAnimationsOfTransition(
    transitions: List<TransitionData>,
    renderData: List<TransitionRenderer?>,
    offsetXGraph: Float,
    offsetYGraph: Float,
    duration: Int = 500,
    onAllAnimationsEnd: () -> Unit,
) {
    if (transitions.isEmpty()) {
        onAllAnimationsEnd()
        return
    }

    val progress = remember { Animatable(0f) }
    val isCanvasVisible = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        )
        isCanvasVisible.value = false
        onAllAnimationsEnd()
    }

    if (isCanvasVisible.value) {
        val circleColor = MaterialTheme.colorScheme.onSurface
        val innerColor = MaterialTheme.colorScheme.surfaceContainerLowest

        data class AnimEntry(val path: Path, val bigR: Float, val smallR: Float)

        val animEntries = transitions.mapNotNull { td ->
            val radiusBig = NODE_RADIUS / 2f
            val radiusSmall = radiusBig - 5f

            // Find matching transition index
            val matchingIdx = this@MultipleAnimationsOfTransition.transitions.indexOfFirst { t ->
                t.startState == td.startStateIndex && t.endState == td.endStateIndex
            }
            if (matchingIdx < 0) return@mapNotNull null
            val rd = renderData.getOrNull(matchingIdx) ?: return@mapNotNull null

            val path = rd.bodyPath
            AnimEntry(path, radiusBig, radiusSmall)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXGraph.roundToInt(), offsetYGraph.roundToInt()) }
        ) {
            for (entry in animEntries) {
                val pos = getCurrentPositionByPath(entry.path, progress.value)
                drawCircle(color = circleColor, radius = entry.bigR, center = pos)
                drawCircle(color = innerColor, radius = entry.smallR, center = pos)
            }
        }
    }
}
