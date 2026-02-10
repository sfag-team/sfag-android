package com.sfag.automata.domain.model.machine

import android.graphics.PathMeasure
import java.util.Locale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sfag.automata.domain.model.simulation.SimulationResult
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.automata.domain.model.tree.DerivationTree
import com.sfag.shared.util.XmlUtils
import com.sfag.automata.domain.model.NODE_RADIUS
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

abstract class Machine(
    val name: String,
    var version: Int,
    val machineType: MachineType,
    val states: MutableList<State>,
    val transitions: MutableList<Transition>,
    var imuInput: StringBuilder = java.lang.StringBuilder(),
    val savedInputs: MutableList<StringBuilder>
) {
    // Snapshot of full input for simulation display
    var fullInputSnapshot: String = ""

    var density: Density? = null

    /**
     * Clears density reference to prevent memory leaks.
     * Call when navigating away from the screen.
     */
    fun clearDensity() {
        density = null
    }
    var input = StringBuilder()
    val derivationTree = DerivationTree()
    var scaleGraph = 1f

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
        if (states.isEmpty()) return null to null
        val currentDensity = density ?: return null to null

        val radius = NODE_RADIUS
        val startPosition =
            if (transition == null) startState!! else getStateByIndex(transition.startState).position
        val endPosition =
            if (transition == null) endState!! else getStateByIndex(transition.endState).position

        val startPoint = startPosition.let { positionDP ->
            return@let with(currentDensity) { (positionDP.x + radius / 2).dp.toPx() to (positionDP.y + radius / 2).dp.toPx() }
        }

        val endPoint = endPosition.let { positionDP ->
            return@let with(currentDensity) { (positionDP.x + radius / 2).dp.toPx() to (positionDP.y + radius / 2).dp.toPx() }
        }
        return if (startPoint == endPoint) {
            val headPosition =
                Offset(
                    endPoint.first - 1.733f * radius,
                    endPoint.second - 0.628f * radius + 18f
                )
            setControlPoint(Offset(startPoint.first, startPoint.second - 2.8f * radius))
            Path().apply {
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

            Path().apply {
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

    private fun getArrowHeadPath(
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
     * Calculates the next simulation step.
     * Returns a SimulationResult that the UI layer uses to display animation.
     * Domain logic only - no UI dependencies.
     */
    abstract fun calculateNextStep(): SimulationResult


    /**
     * Returns state with the given index, or throws if not found.
     * Use getStateByIndexOrNull for safe access.
     */
    fun getStateByIndex(index: Int): State = states.firstOrNull { it.index == index }
        ?: throw IllegalArgumentException("State with index $index not found")

    /**
     * Returns state with the given index, or null if not found.
     */
    fun getStateByIndexOrNull(index: Int): State? = states.firstOrNull { it.index == index }


    fun getDpOffsetWithPxOffset(pxPosition: Offset): Offset? {
        val currentDensity = density ?: return null
        return Offset(
            String.format(Locale.US, "%.2f", (pxPosition.x) / currentDensity.density).toFloat(),
            String.format(Locale.US, "%.2f", (pxPosition.y) / currentDensity.density).toFloat()
        )
    }

    fun setInitialStateAsCurrent() {
        currentState?.let { stateIndex ->
            getStateByIndexOrNull(stateIndex)?.isCurrent = false
        }
        derivationTree.clear()
        states.firstOrNull { it.initial }?.let { initialState ->
            initialState.isCurrent = true
            currentState = initialState.index
            derivationTree.initialize(initialState.name)
        }
        // Use polymorphism - each machine type handles its own reset
        resetMachineState()
    }

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


    abstract fun expandDerivationTree()

    /**
     * Returns the formal mathematical definition data for this machine.
     * UI layer uses this to render the mathematical representation.
     */
    abstract fun getMathFormatData(): MachineFormatData

    abstract fun canReachFinalState(input: StringBuilder, fromInit:Boolean): Boolean

    /**
     * Exports machine to JFLAP-compatible JFF format.
     * Uses template method pattern - subclasses implement exportTransitionsToJFF().
     */
    fun exportToJFF(): String {
        val builder = StringBuilder()
        builder.appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>""")
        builder.appendLine("<structure>")
        builder.appendLine("    <type>${machineType.tag}</type>")
        builder.appendLine("    <automaton>")

        // Export states (common for all machine types)
        for (state in states) {
            val escapedName = XmlUtils.escapeXml(state.name)
            builder.appendLine("""        <state id="${state.index}" name="$escapedName">""")
            builder.appendLine("""            <x>${XmlUtils.formatFloat(state.position.x)}</x>""")
            builder.appendLine("""            <y>${XmlUtils.formatFloat(state.position.y)}</y>""")
            if (state.initial) builder.appendLine("            <initial/>")
            if (state.finite) builder.appendLine("            <final/>")
            builder.appendLine("        </state>")
        }

        // Export transitions (machine-type specific)
        exportTransitionsToJFF(builder)

        builder.appendLine("    </automaton>")
        builder.appendLine("</structure>")

        return builder.toString()
    }

    /**
     * Exports transitions to JFF format. Override in subclasses for machine-specific transition format.
     */
    protected abstract fun exportTransitionsToJFF(builder: StringBuilder)

    /**
     * Called when resetting simulation to initial state.
     * Override in subclasses to reset machine-specific state (e.g., stack, tape).
     */
    open fun resetMachineState() {
        // Base implementation does nothing - override in subclasses
    }
}
