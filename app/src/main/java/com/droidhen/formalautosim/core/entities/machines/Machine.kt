package com.droidhen.formalautosim.core.entities.machines

import android.annotation.SuppressLint
import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidhen.formalautosim.R
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition
import com.droidhen.formalautosim.presentation.theme.light_blue
import com.droidhen.formalautosim.presentation.views.DefaultTextField
import com.droidhen.formalautosim.presentation.views.FASButton
import com.droidhen.formalautosim.utils.extensions.drawArrow
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Suppress("UNREACHABLE_CODE")
abstract class Machine(var name: String = "Untitled") {
    val states = mutableListOf<State>()
    protected val transitions = mutableListOf<Transition>()
    val input = StringBuilder()
    private var offsetXGraph = 0f
    private var offsetYGraph = 0f
    abstract var currentState: Int?


    /**
     * return all paths for all exists transitions
     *
     * @param density - screen parameter needed for correct calculation regarding screen size
     * @return list of pairs path to path - the first path - path of arrow, second one - path for arrow head
     */
    private fun getAllPath(
        density: Density,
        setControlPoint: (Offset) -> Unit,
        setTransition: (Transition) -> Unit,
    ): List<Pair<Path?, Path?>?> {
        val listOfPaths = arrayOfNulls<Pair<Path?, Path?>>(transitions.size)
        transitions.forEach { transition ->
            setTransition(transition)
            listOfPaths[transitions.indexOf(transition)] =
                getTransitionByPath(density, transition) { controlPoint ->
                    setControlPoint(controlPoint)
                }
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
        transition: Transition? = null,
        startState: Offset? = null,
        endState: Offset? = null,
        primaryCurvature: Int = 100,
        setControlPoint: (Offset) -> Unit,
    ): Pair<Path?, Path?> {
        if (transition != null && !transitions.contains(transition)) return null to null


        val radius = states[0].radius
        val startPosition =
            if (transition == null) startState!! else getStateByIndex(transition.startState).position
        val endPosition =
            if (transition == null) endState!! else getStateByIndex(transition.endState).position

        val startPoint = startPosition.let { positionDP ->
            return@let with(density) { (positionDP.x + radius / 2).dp.toPx() to (positionDP.y + radius / 2).dp.toPx() }
        }

        val endPoint = endPosition.let { positionDP ->
            return@let with(density) { (positionDP.x + radius / 2).dp.toPx() to (positionDP.y + radius / 2).dp.toPx() }
        }
        return if (startPoint == endPoint) {
            val headPosition =
                Offset(
                    endPoint.first - 1.733f * radius,
                    endPoint.second - 0.628f * radius + 18f
                )
            setControlPoint(Offset(startPoint.first, startPoint.second - 2.8f * radius))
            return Path().apply {
                addOval(
                    Rect(
                        center = Offset(
                            x = startPoint.first,
                            y = startPoint.second - radius
                        ), radius = radius * 1.4f
                    )
                )
            } to getArrowHeadPath(headPosition, -0.9f, 0.436f, dirX = 1f, dirY = 0f)
        } else {
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
            setControlPoint(controlPoint)
            val angleRadians = atan((length / 2) / curvature)
            val cosTheta = cos(angleRadians)
            val sinTheta = sin(angleRadians)

            return Path().apply {
                moveTo(startOffset.x, startOffset.y)
                quadraticBezierTo(controlPoint.x, controlPoint.y, endOffset.x, endOffset.y)
            } to getArrowHeadPath(
                Offset(endOffset.x, endOffset.y),
                sinTheta,
                cosTheta,
                dirX = dirX,
                dirY = dirY
            )
        }
    }

    fun getArrowHeadPath(
        position: Offset,
        sin: Float,
        cos: Float,
        size: Float = 26f,
        dirX: Float,
        dirY: Float,
    ): Path {
        return Path().apply {
            val halfSize = size / 2

            fun rotateVector(dirX: Float, dirY: Float): Pair<Float, Float> {
                val rotatedX = dirX * sin - dirY * cos
                val rotatedY = dirX * cos + dirY * sin
                return rotatedX to rotatedY
            }

            val (rotatedDirX, rotatedDirY) = rotateVector(dirX, dirY)
            val (rotatedPerpDirX, rotatedPerpDirY) = rotateVector(-dirY, dirX)

            moveTo(position.x, position.y)

            lineTo(
                position.x - size * rotatedDirX - halfSize * rotatedPerpDirX,
                position.y - size * rotatedDirY - halfSize * rotatedPerpDirY
            )

            lineTo(
                position.x - size * rotatedDirX + halfSize * rotatedPerpDirX,
                position.y - size * rotatedDirY + halfSize * rotatedPerpDirY
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
    @SuppressLint("ComposableNaming")
    abstract fun calculateTransition(onAnimationEnd: () -> Unit)

    /**
     * convert machine to key - value String format for saving machine on relative database
     */
    abstract fun convertMachineToKeyValue(): List<Pair<String, String>>

    /**
     * @returns state with the same index, if it exists, else - returns null
     */
    fun getStateByIndex(index: Int?): State = states.filter {
        it.index == index
    }[0]


    /**
     * draws machine with all states and transitions
     */
    @SuppressLint("ComposableNaming", "SuspiciousIndentation")
    @Composable
    fun drawMachine() {
        var offsetX by remember {
            mutableFloatStateOf(offsetXGraph)
        }
        var offsetY by remember {
            mutableFloatStateOf(offsetYGraph)
        }

        val dragModifier = Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
                offsetY += dragAmount.y
                offsetXGraph = offsetX
                offsetYGraph = offsetY
            }
        }

        InputBar()
        Transitions(dragModifier = dragModifier, offsetY, offsetX)
        States(dragModifier = dragModifier, offsetY, offsetX)
    }


    /**
     * private compose function States
     *
     * composes all states of machine on the screen
     * @param dragModifier needed for correct drag and drop of states
     *
     */
    @Composable
    private fun States(
        @SuppressLint("ModifierParameter") dragModifier: Modifier,
        offsetY: Float,
        offsetX: Float,
        borderColor: Color = MaterialTheme.colorScheme.tertiary,
    ) {
        val currentCircleColor = MaterialTheme.colorScheme.primaryContainer
        states.forEach { state ->
            Box(
                modifier = Modifier
                    .size(state.radius.dp)
                    .offset(state.position.x.dp, state.position.y.dp)
            ) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .then(dragModifier)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }) {
                    drawCircle(
                        color = borderColor,
                        radius = state.radius + 1,
                        style = Stroke(width = 10f)
                    )
                    drawCircle(
                        color = if (state.isCurrent) currentCircleColor else Color.White,
                        radius = if (state.isCurrent) state.radius - 1 else state.radius
                    )
                    if (state.finite) {
                        drawCircle(
                            color = borderColor,
                            radius = state.radius - 10,
                            style = Stroke(width = 5f)
                        )
                        drawCircle(
                            color = if (state.isCurrent) currentCircleColor else Color.White,
                            radius = if (state.isCurrent) state.radius - 12 else state.radius - 11
                        )
                    }
                    if (state.initial) {
                        val OFFSET = 30f
                        val scaleFactor = 3f

                        val arrowPath = Path().apply {
                            moveTo(size.width * 0.1f - OFFSET * 2, size.height / 2)
                            lineTo(size.width * 0.4f - OFFSET, size.height / 2)
                            lineTo(
                                size.width * 0.35f - OFFSET * 2,
                                size.height / 2 - size.height * 0.05f * scaleFactor
                            )
                            moveTo(size.width * 0.4f - OFFSET, size.height / 2)
                            lineTo(
                                size.width * 0.35f - OFFSET * 2,
                                size.height / 2 + size.height * 0.05f * scaleFactor
                            )
                        }

                        drawPath(
                            path = arrowPath,
                            color = Color.Black,
                            style = Stroke(width = 5f)
                        )
                    }
                }
                Text(
                    text = state.name,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(dragModifier)
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
                    style = TextStyle(color = MaterialTheme.colorScheme.tertiary),
                    fontSize = 20.sp
                )
            }
        }
    }


    /**
     * private compose function Transitions
     *
     * composes all transitions of machine
     * @param dragModifier needed for correct drag and drop of transitions
     */
    @Composable
    private fun Transitions(
        dragModifier: Modifier,
        offsetY: Float,
        offsetX: Float,
        borderColor: Color = MaterialTheme.colorScheme.tertiary,
    ) {
        val controlPointList = mutableListOf<Offset>()
        val transitionLocalList = mutableListOf<Transition>()
        val paths = getAllPath(LocalDensity.current, { controlPoint ->
            controlPointList.add(controlPoint)
        }, { transition ->
            transitionLocalList.add(transition)
        })
        Canvas(modifier = Modifier
            .fillMaxSize()
            .then(dragModifier)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }) {
            paths.forEach { path ->
                val controlPoint = controlPointList[paths.indexOf(path)]
                drawArrow(path!!, borderColor)
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        transitionLocalList[paths.indexOf(path)].name,
                        controlPoint.x,
                        controlPoint.y,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 40f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }


    /**
     * InputBar
     *
     * Compose function that creates bar that shows input chars for the machine
     */
    @Composable
    private fun InputBar() {
        var iteration = 0
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(light_blue)
                    .padding(start = 8.dp)
                    .clickable {

                    },
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                input.toString().toCharArray().toList().forEach { inputChar ->
                    iteration++
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (iteration == 1) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            inputChar.toString(),
                            fontSize = 25.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                }
            }
        }
    }


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
    protected fun AnimationOfTransition(
        start: Offset,
        end: Offset,
        radius: Float,
        duration: Int = 4500,
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
            val density = LocalDensity.current
            val radiusBigCircle = radius / 2
            val radiusSmallCircle = radiusBigCircle - 5
            val path = getTransitionByPath(density, startState = start, endState = end) {}

            Canvas(modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXGraph.roundToInt(), offsetYGraph.roundToInt()) }) {

                val currentPosition = getCurrentPositionByPath(
                    path.first!!,
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

    /**
     * return position of transition point by path and progress of point had made
     * @param path - path of point
     * @param progress - progress of point had made
     *
     * @return Offset - position of point
     */
    private fun getCurrentPositionByPath(path: Path, progress: Float): Offset {
        val currentPositionArray = FloatArray(2)
        val pathMeasure = PathMeasure(path.asAndroidPath(), false)
        pathMeasure.getPosTan(pathMeasure.length * progress, currentPositionArray, null)
        return Offset(currentPositionArray[0], currentPositionArray[1])
    }


    /**
     * Screen for editing input bar content
     *
     * @param finishedEditing it's a lambda - that invokes when user confirm his changes
     */
    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun EditingInput(finishedEditing: () -> Unit){
        val inputValue = mutableStateOf(input.toString())
        Column (Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.size(32.dp))
            Text(text = stringResource(R.string.editing_input_headline), style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier =Modifier.size(16.dp))
            DefaultTextField(
                hint = "",
                value = inputValue.value.reversed(),
                requirementText = stringResource(R.string.requirement_text_for_machine_input),
                onTextChange = { newInput ->
                    input.clear()
                    input.append(newInput.reversed())
                    inputValue.value = newInput
                }) {
                input.contains("^[A-Za-z]+$".toRegex())
            }

            Spacer(modifier = Modifier.fillMaxHeight(0.7f))

            FASButton(text = "Confirm", onClick = finishedEditing)
        }
    }
}