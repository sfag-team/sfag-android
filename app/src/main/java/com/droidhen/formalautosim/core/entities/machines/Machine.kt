package com.droidhen.formalautosim.core.entities.machines

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animation
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition
import com.droidhen.formalautosim.utils.extensions.drawArrow
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

abstract class Machine(var name: String = "Untitled") {
    val states = mutableListOf<State>()
    protected val transitions = mutableListOf<Transition>()
    val input = ArrayDeque<Char>()
    abstract var currentState: Int?


    /**
     * return all paths for all exists transitions
     *
     * @param density - screen parameter needed for correct calculation regarding screen size
     * @return list of pairs path to path - the first path - path of arrow, second one - path for arrow head
     */
    private fun getAllPath(density: Density): List<Pair<Path?, Path?>?> {
        val listOfPaths = arrayOfNulls<Pair<Path?, Path?>>(transitions.size)
        transitions.forEach { transition ->
            listOfPaths[transitions.indexOf(transition)] = getTransitionByPath(density, transition)
        }
        return listOfPaths.toList()
    }

    /**
     * create path for composing arrow on the screen
     *
     * @param density - screen parameter needed for correct calculation regarding screen size
     * @param transition - pair (evidence num of start state to ev. num of destination state)
     * @return pair Path to arrow, Path - to arrow head, in case that between states path doesn't exists - return pair null to null
     */
    fun getTransitionByPath(
        density: Density,
        transition: Transition,
        primaryCurvature: Int = 100,
    ): Pair<Path?, Path?> {
        if (!transitions.contains(transition)) return null to null
        val radius = states[0].radius
        val startPoint =
            getStateByIndex(transition.startState).position.let { positionDP ->
                return@let with(density) { (positionDP.x + radius / 2).dp.toPx() to (positionDP.y + radius / 2).dp.toPx() }
            }
        val endPoint =
            getStateByIndex(transition.endState).position.let { positionDP ->
                return@let with(density) { (positionDP.x + radius / 2).dp.toPx() to (positionDP.y + radius / 2).dp.toPx() }
            }
        val dx = endPoint.first - startPoint.first
        val dy = endPoint.second - startPoint.second
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        val curvature = primaryCurvature * length / 1000
        val dirX = dx / length
        val dirY = dy / length

        val startOffset = Offset(
            startPoint.first + radius * dirX,
            startPoint.second + radius * dirY
        )
        val endOffset = Offset(
            endPoint.first - radius * dirX,
            endPoint.second - radius * dirY
        )

        val controlPoint = Offset(
            (startPoint.first + endPoint.first) / 2 + curvature * dirY,
            (startPoint.second + endPoint.second) / 2 - curvature * dirX
        )

        return Path().apply {
            moveTo(startOffset.x, startOffset.y)
            quadraticBezierTo(controlPoint.x, controlPoint.y, endOffset.x, endOffset.y)
        } to Path().apply {
            val angleRadians = atan((length / 2) / curvature)
            val cosTheta = cos(angleRadians)
            val sinTheta = sin(angleRadians)
            val size = 26
            val halfSize = size / 2

            fun rotateVector(x: Float, y: Float): Pair<Float, Float> {
                val rotatedX = x * sinTheta - y * cosTheta
                val rotatedY = x * cosTheta + y * sinTheta
                return rotatedX to rotatedY
            }
            val (rotatedDirX, rotatedDirY) = rotateVector(dirX, dirY)

            val (rotatedPerpDirX, rotatedPerpDirY) = rotateVector(-dirY, dirX)
            moveTo(endOffset.x, endOffset.y)

            lineTo(
                endOffset.x - size * rotatedDirX - halfSize * rotatedPerpDirX,
                endOffset.y - size * rotatedDirY - halfSize * rotatedPerpDirY
            )

            lineTo(
                endOffset.x - size * rotatedDirX + halfSize * rotatedPerpDirX,
                endOffset.y - size * rotatedDirY + halfSize * rotatedPerpDirY
            )
            close()
        }
    }

    /**
     * add new transition to machine
     *
     * @param transition - pair Int to Int where Int - evidence num of state
     */
    fun addTransition(transition: Transition) {
        if (transitions.contains(transition)) return
        if (states.any { transition.startState == it.index } && states.any { transition.endState == it.index }) {
            transitions.add(transition)
        }
    }

    /**
     * delete transition
     *
     * @param transition - pair Int to Int where Int - evidence num of state
     */
    fun deleteTransition(transition: Transition) {
        transitions.remove(transition)
    }

    /**
     * simulate transition regarding current state and input
     */
    @Composable
    abstract fun simulateTransition()

    /**
     * convert machine to key - value String format for saving machine on relative database
     */
    abstract fun convertMachineToKeyValue(): List<Pair<String, String>>

    /**
     * @returns state with the same index, if it exists, else - returns null
     */
    fun getStateByIndex(index: Int): State = states.filter {
        it.index == index
    }[0]


    /**
     * draws machine with all states and transitions
     */
    @SuppressLint("ComposableNaming")
    @Composable
    fun drawMachine() {
        val currentCircleColor = MaterialTheme.colorScheme.primaryContainer
        val borderColor = MaterialTheme.colorScheme.tertiary
        states.forEach { state ->
            getAllPath(LocalDensity.current).forEach { path ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArrow(path!!, borderColor)
                }
            }
            Box(
                modifier = Modifier
                    .size(state.radius.dp)
                    .offset(state.position.x.dp, state.position.y.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = borderColor,
                        radius = state.radius + 1,
                        style = Stroke(width = 10f)
                    )
                    drawCircle(
                        color = if (state.isCurrent) currentCircleColor else Color.White,
                        radius = if (state.isCurrent) state.radius - 3 else state.radius
                    )
                }
                Text(
                    text = state.name,
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(color = MaterialTheme.colorScheme.tertiary),
                    fontSize = 20.sp
                )
            }
        }
    }

    @Composable
    protected fun AnimationOfTransition(
        start: Offset,
        end: Offset,
        curvature: Int = 200,
        duration: Int = 500,
    ) {
        val progress = rememberInfiniteTransition(label = "").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(duration, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = ""
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val currentPosition = getPositionByAnimationProgress(progress.value, start, end, curvature)

            drawCircle(color = Color.Red, radius = 15f, center = currentPosition)
        }


    }

    private fun getPositionByAnimationProgress(
        progress: Float,
        start: Offset,
        end: Offset,
        curvature: Int,
    ) = Offset(
        start.x + (end.x - start.x) * progress,
        start.y + (end.y - start.y) * progress + curvature * (1 - 2 * progress) * progress
    )

}