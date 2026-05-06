package com.sfag.automata.ui.machine

import androidx.compose.animation.core.Animatable
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
import com.sfag.automata.domain.simulation.TransitionRef
import com.sfag.automata.ui.common.NODE_RADIUS
import kotlin.math.roundToInt

/**
 * Animates one or more transitions simultaneously during a simulation step. All circles share a
 * single progress value so they move in lockstep.
 */
@Composable
fun TransitionAnimation(
    transitionRefs: List<TransitionRef>,
    transitionPaths: List<TransitionPath?>,
    offsetXCanvas: Float,
    offsetYCanvas: Float,
    duration: Int = 400,
    onAnimationsEnd: () -> Unit,
) {
    if (transitionRefs.isEmpty()) {
        onAnimationsEnd()
        return
    }

    val progress = remember { Animatable(0f) }
    val isCanvasVisible = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        progress.animateTo(targetValue = 1f, animationSpec = tween(duration))
        isCanvasVisible.value = false
        onAnimationsEnd()
    }

    if (isCanvasVisible.value) {
        val circleColor = MaterialTheme.colorScheme.onSurface
        val innerColor = MaterialTheme.colorScheme.surfaceContainerLowest

        data class AnimEntry(val path: Path, val bigR: Float, val smallR: Float)

        val animEntries =
            transitionRefs.mapNotNull { ref ->
                val transitionPath =
                    transitionPaths.getOrNull(ref.transitionIndex) ?: return@mapNotNull null

                val path = transitionPath.arrowBody
                val radiusBig = NODE_RADIUS / 2f
                val radiusSmall = radiusBig - 5f

                AnimEntry(path, radiusBig, radiusSmall)
            }

        Canvas(
            modifier =
                Modifier.fillMaxSize().offset {
                    IntOffset(offsetXCanvas.roundToInt(), offsetYCanvas.roundToInt())
                }
        ) {
            for (entry in animEntries) {
                val position = getCurrentPositionByPath(entry.path, progress.value)
                drawCircle(color = circleColor, radius = entry.bigR, center = position)
                drawCircle(color = innerColor, radius = entry.smallR, center = position)
            }
        }
    }
}
