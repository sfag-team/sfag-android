package com.sfag.automata.ui.component.simulation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.simulation.TransitionData
import kotlin.math.roundToInt


/**
 * Animates a value based on the current state of a transition.
 *
 * This function creates a [State] object that animates a value between 0f and 1f
 * as the provided [transition] progresses between states. The animation is driven
 * by the [targetState] and the current state of the transition.
 *
 * @param targetState The target state for the animation. The animation will
 * progress towards 1f when the transition is in this state.
 * @param transition The transition that drives the animation.
 * @param label An optional label for the animation, used for debugging purposes.
 *
 * @return A [State] object that represents the animated value.
 */
@Composable
fun Machine.AnimationOfTransition(
    start: Offset,
    end: Offset,
    radius: Float,
    duration: Int = 500,
    onAnimationEnd: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val isCanvasVisible = remember {
        mutableStateOf(true)
    }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        )
        isCanvasVisible.value = false
        onAnimationEnd()
    }

    if (isCanvasVisible.value) {
        val circleColor = MaterialTheme.colorScheme.primary
        val radiusBigCircle = radius / 2
        val radiusSmallCircle = radiusBigCircle - 5
        val path = getTransitionByPath(startState = start, endState = end) {}
        val animationPath = path.first

        if (animationPath != null) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXGraph.roundToInt(), offsetYGraph.roundToInt()) }) {

                val currentPosition = getCurrentPositionByPath(
                    animationPath,
                    progress.value * if (start == end) 0.8f else 1f
                )
                drawCircle(color = circleColor, radius = radiusBigCircle, center = currentPosition)
                drawCircle(
                    color = Color.White,
                    radius = radiusSmallCircle,
                    center = currentPosition
                )
            }
        }
    }
}

/**
 * Animates multiple transitions simultaneously for NFA simulation.
 * All transitions animate in parallel, and onAllAnimationsEnd is called
 * when all animations complete.
 */
@Composable
fun Machine.MultipleAnimationsOfTransition(
    transitions: List<TransitionData>,
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
        val circleColor = MaterialTheme.colorScheme.primary

        Canvas(modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetXGraph.roundToInt(), offsetYGraph.roundToInt()) }
        ) {
            // Draw animation for each transition
            for (transitionData in transitions) {
                val radiusBigCircle = transitionData.radius / 2
                val radiusSmallCircle = radiusBigCircle - 5
                val path = getTransitionByPath(
                    startState = transitionData.startPosition,
                    endState = transitionData.endPosition
                ) {}
                val animationPath = path.first

                if (animationPath != null) {
                    val currentPosition = getCurrentPositionByPath(
                        animationPath,
                        progress.value * if (transitionData.startPosition == transitionData.endPosition) 0.8f else 1f
                    )
                    drawCircle(
                        color = circleColor,
                        radius = radiusBigCircle,
                        center = currentPosition
                    )
                    drawCircle(
                        color = Color.White,
                        radius = radiusSmallCircle,
                        center = currentPosition
                    )
                }
            }
        }
    }
}
