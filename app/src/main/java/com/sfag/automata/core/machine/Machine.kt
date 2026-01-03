package com.sfag.automata.core.machine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PathMeasure
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sfag.automata.core.state.State
import com.sfag.automata.core.transition.PushDownTransition
import com.sfag.automata.core.transition.Transition
import com.sfag.automata.core.tree.TreeNode
import com.sfag.automata.util.enum.EditMachineStates
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin


@Suppress("UNREACHABLE_CODE")
abstract class Machine(
    val name: String,
    var version: Int,
    val machineType: MachineType,
    val states: MutableList<State>,
    val transitions: MutableList<Transition>,
    var imuInput: StringBuilder = java.lang.StringBuilder(),
    val savedInputs: MutableList<StringBuilder>
) {

    lateinit var density: Density
    lateinit var context: Context
    var globalCanvasPosition: LayoutCoordinates? = null
    var input = StringBuilder()
    var currentTreePosition = 1

    var offsetXGraph = 0f
    var offsetYGraph = 0f
    var editMode = EditMachineStates.ADD_STATES
    abstract var currentState: Int?

    var onBottomTransitionClicked: (Transition) -> Unit = {}
    var onBottomStateClicked: (State) -> Unit = {}


    /**
     * return all paths for all exists transitions
     *
     * @return list of pairs path to path - the first path - path of arrow, second one - path for arrow head
     */
    fun getAllPath(
        setTransition: (Transition) -> Unit,
    ): List<Pair<Path?, Path?>?> {
        val listOfPaths = arrayOfNulls<Pair<Path?, Path?>>(transitions.size)
        transitions.forEach { transition ->
            setTransition(transition)
            listOfPaths[transitions.indexOf(transition)] =
                getTransitionByPath(transition) { controlPoint ->
                    transition.controlPoint = controlPoint
                }
        }
        return listOfPaths.toList()
    }

    /**
     * create path for composing arrow on the screen
     *
     * @param transition - pair (evidence num of start state to ev. num of destination state)
     * @return pair Path to arrow, Path - to arrow head, in case that between states path doesn't exists - return pair null to null
     */
    fun getTransitionByPath(
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
                quadraticTo(controlPoint.x, controlPoint.y, endOffset.x, endOffset.y)
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
     * delete transition
     *
     * @param transition - pair Int to Int where Int - evidence num of state
     */
    fun deleteTransition(transition: Transition) {
        transitions.remove(transition)
    }

    /**
     * delete state
     *
     * @param state - state that should be deleted
     */
    fun deleteState(state: State) {
        states.remove(state)
        transitions.filter { it.startState == state.index || it.endState == state.index }
            .forEach { transitionToRemove ->
                transitions.remove(transitionToRemove)
            }
    }

    /**
     * simulate transition regarding current state and input
     */
    @Composable
    @SuppressLint("ComposableNaming")
    abstract fun calculateTransition(onAnimationEnd: (Boolean?) -> Unit)

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


    @SuppressLint("DefaultLocale")
    fun getDpOffsetWithPxOffset(pxPosition: Offset): Offset {
        return Offset(
            String.format(
                "%.2f",
                (pxPosition.x) / density.density
            ).replace(',', '.').toFloat(),
            String.format(
                "%.2f",
                (pxPosition.y) / density.density
            ).replace(',', '.').toFloat()
        )
    }

    /**
     * Screen for editing input bar content
     *
     * @param finishedEditing it's a lambda - that invokes when user confirm his changes
     */

    fun setInitialStateAsCurrent() {
        currentState?.let {
            getStateByIndex(it).isCurrent = false
        }
        val initialState = states.filter { it.initial }
        if (initialState.isNotEmpty()) {
            initialState[0].isCurrent = true
            currentState = initialState[0].index
            currentTreePosition = 1
        }
        if (machineType == MachineType.Pushdown) {
            (this as PushDownMachine).symbolStack.clear()
            this.symbolStack.add('Z')
        }
    }

    /**
     * private compose function States
     *
     * composes all states of machine on the screen
     * @param dragModifier needed for correct drag and drop of states
     *
     */


    /**
     * private compose function Transitions
     *
     * composes all transitions of machine
     * @param dragModifier needed for correct drag and drop of transitions
     */


    abstract fun addNewState(state: State)

    /**
     * checks if machine already has state with the same name
     * if so - modifies already existing state (extends name of existing state)
     * behaviour of this function can be changed in children of Machine class
     *
     * @param name - name of new state
     * @param startState and endState - states of transition
     */
    fun addNewTransition(name: String, startState: State, endState: State) {
        var iterations = 0
        transitions.filter { transition ->
            transition.startState == startState.index && transition.endState == endState.index && transition.name == name
        }.forEach {
            iterations++
            it.name += name
        }
        if (iterations == 0) {
            transitions.add(Transition(name, startState.index, endState.index))
        }
    }

    fun addNewTransition(
        name: String,
        startState: State,
        endState: State,
        pop: String,
        checkState: String,
        push: String
    ) {

        transitions.add(
            if (pop != "") {
                if (transitions.any {
                        it as PushDownTransition
                        it.name == name && it.startState == startState.index && it.endState == endState.index && it.pop == pop && it.push == ""
                    }) return
                PushDownTransition(
                    name,
                    startState.index,
                    endState.index,
                    pop = pop,
                    push = "",
                )
            } else if (checkState != "") {
                if (transitions.any {
                        it as PushDownTransition
                        it.name == name && it.startState == startState.index && it.endState == endState.index && it.pop == checkState && it.push == push + checkState
                    }) return
                PushDownTransition(
                    name,
                    startState.index,
                    endState.index,
                    pop = checkState,
                    push = push + checkState
                )
            } else {
                PushDownTransition(name, startState.index, endState.index, pop = "", push = push)
            }
        )
    }


    /**
     * InputBar
     *
     * Compose function that creates bar that shows input chars for the machine
     */


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

    /**
     * return position of transition point by path and progress of point had made
     * @param path - path of point
     * @param progress - progress of point had made
     *
     * @return Offset - position of point
     */
    fun getCurrentPositionByPath(path: Path, progress: Float): Offset {
        val currentPositionArray = FloatArray(2)
        val pathMeasure = PathMeasure(path.asAndroidPath(), false)
        pathMeasure.getPosTan(pathMeasure.length * progress, currentPositionArray, null)
        return Offset(currentPositionArray[0], currentPositionArray[1])
    }


    abstract fun getDerivationTreeElements(): List<List<TreeNode>>


    @Composable
    abstract fun MathFormat()

    abstract fun canReachFinalState(input: StringBuilder, fromInit:Boolean): Boolean

    abstract fun exportToJFF(): String
}

sealed class MachineType(val tag: String) {
    object Finite : MachineType("fa")
    object Pushdown : MachineType("pda")

    override fun toString(): String = tag
}
