package com.sfag.automata.domain.model.machine

import com.sfag.automata.domain.model.simulation.SimulationResult
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.automata.domain.model.tree.TreeNode
import com.sfag.shared.util.XmlUtils.escapeXml
import com.sfag.shared.util.XmlUtils.formatFloat


class FiniteMachine(
    name: String = "Untitled", version: Int = 1, states: MutableList<State> = mutableListOf(),
    transitions: MutableList<Transition> = mutableListOf(), savedInputs: MutableList<StringBuilder> = mutableListOf()
) : Machine(
    name, version,
    machineType = MachineType.Finite,
    states, transitions, savedInputs = savedInputs
) {
    override var currentState: Int? = null

    override fun calculateNextStep(): SimulationResult {
        if (currentState == null) currentState = states.firstOrNull { it.initial }?.index
        val currentStateIndex = currentState
            ?: return SimulationResult.Ended(null)

        val startState = getStateByIndex(currentStateIndex)

        val possibleTransitions = getListOfAppropriateTransitions(startState)
        if (possibleTransitions.isEmpty()) {
            return SimulationResult.Ended(startState.finite)
        }

        var validTransition: Transition? = possibleTransitions.firstOrNull { transition ->
            val nextInput = input.removePrefix(transition.name)
            val nextState = getStateByIndex(transition.endState)

            val previousCurrent = currentState ?: return@firstOrNull false
            states.forEach { it.isCurrent = false }
            nextState.isCurrent = true
            currentState = nextState.index

            val result = canReachFinalState(StringBuilder(nextInput), false)

            nextState.isCurrent = false
            getStateByIndexOrNull(previousCurrent)?.isCurrent = true
            currentState = previousCurrent

            result
        }

        if (validTransition == null) {
            validTransition = possibleTransitions.first()
        }

        val endState = getStateByIndex(validTransition.endState)
        val newInputValue = input.removePrefix(validTransition.name).toString()
        input.clear()
        input.append(newInputValue)

        val consumed = validTransition.name.length
        if (consumed > 0 && imuInput.isNotEmpty()) {
            imuInput.delete(0, minOf(consumed, imuInput.length))
        }
        currentTreePosition++

        return SimulationResult.Transition(
            startPosition = startState.position,
            endPosition = endState.position,
            radius = startState.radius,
            onComplete = {
                startState.isCurrent = false
                endState.isCurrent = true
                currentState = endState.index
            }
        )
    }

    /**
     * Checks if input was fully consumed and machine is in accepting state.
     */
    fun isAccepted(): Boolean? {
        val currentIdx = currentState ?: return null
        val state = getStateByIndexOrNull(currentIdx) ?: return null
        return if (input.isEmpty() && state.finite) true else null
    }

    override fun addNewState(state: State) {
        if (state.initial && currentState == null) {
            currentState = state.index
            state.isCurrent = true
        }
        states.add(state)
    }

    /**
     * Creates list of map that represents tree
     * Each map it's a level of the tree, Key - name of transition, Float - number of leaves under this state
     *
     */
    override fun getDerivationTreeElements(): List<List<TreeNode>> {
        val allPaths = mutableListOf<List<String?>>()

        data class Path(
            val history: List<String?>,
            val currentState: State?,
            val inputIndex: Int,
            val alive: Boolean
        )

        val startStates = states.filter { it.initial }
        var paths = startStates.map {
            Path(listOf(null), it, 0, true)
        }.toMutableList()

        while (paths.any { it.alive }) {
            val nextPaths = mutableListOf<Path>()

            paths.forEach { path ->
                if (!path.alive) {
                    nextPaths.add(Path(path.history + null, null, path.inputIndex, false))
                    return@forEach
                }

                if (path.inputIndex == imuInput.length) {
                    allPaths.add(path.history + path.currentState?.name)
                    nextPaths.add(Path(path.history + null, null, path.inputIndex, false))
                    return@forEach
                }

                val currentChar = imuInput[path.inputIndex]
                val possibleTransitions = transitions.filter {
                    it.startState == path.currentState?.index && (it.name.isEmpty() || it.name.firstOrNull() == currentChar)
                }

                if (possibleTransitions.isEmpty()) {
                    allPaths.add(path.history + path.currentState?.name)
                    nextPaths.add(Path(path.history + null, null, path.inputIndex, false))
                    return@forEach
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    nextPaths.add(
                        Path(
                            path.history + path.currentState?.name,
                            nextState,
                            path.inputIndex + 1,
                            true
                        )
                    )
                }
            }

            paths = nextPaths
        }

        val acceptedPaths = allPaths.filter { path ->
            val last = path.lastOrNull()
            last != null && states.any { it.name == last && it.finite }
        }

        val maxDepth = allPaths.maxOfOrNull { it.size } ?: 0
        val normalizedPaths = allPaths.map { path ->
            buildList {
                addAll(path)
                while (size < maxDepth) add(null)
            }
        }

        val tree = mutableListOf<List<TreeNode>>()

        for (level in 1 until maxDepth) {
            val nodeMap = mutableMapOf<String?, MutableList<Int>>()
            normalizedPaths.forEachIndexed { pathIndex, path ->
                val stateName = path[level]
                if (stateName !in nodeMap) nodeMap[stateName] = mutableListOf()
                nodeMap[stateName]?.add(pathIndex)
            }

            val levelNodes = nodeMap.map { (stateName, indices) ->
                val weight = indices.size.toFloat()
                val isAccepted = indices.any { acceptedPaths.contains(normalizedPaths[it]) }
                val isCurrent = (stateName != null && states.firstOrNull { it.name == stateName }?.isCurrent == true) && currentTreePosition == level

                TreeNode(
                    stateName = stateName,
                    weight = weight,
                    isCurrent = isCurrent,
                    isAccepted = isAccepted
                )
            }

            tree.add(levelNodes)
        }

        return tree
    }

    override fun getMathFormatData(): MachineFormatData {
        val transitionDescriptions = transitions.map { t ->
            val fromState = states.find { it.index == t.startState }?.name ?: "?"
            val toState = states.find { it.index == t.endState }?.name ?: "?"
            val readSymbol = t.name.ifEmpty { "ε" }
            "δ($fromState, $readSymbol) = $toState"
        }

        return MachineFormatData(
            stateNames = states.map { it.name },
            inputAlphabet = transitions.mapNotNull { it.name.firstOrNull() }.toSet(),
            initialStateName = states.firstOrNull { it.initial }?.name ?: "q₀",
            finalStateNames = states.filter { it.finite }.map { it.name },
            transitionDescriptions = transitionDescriptions,
            machineType = machineType
        )
    }

    override fun canReachFinalState(input: StringBuilder, fromInit:Boolean): Boolean {
        data class Path(
            val currentState: State,
            val inputIndex: Int
        )

        var paths = mutableListOf<Path>()
        var startState = states.firstOrNull { if(!fromInit) it.isCurrent else it.initial}
        if(startState==null){
            setInitialStateAsCurrent()
            startState = states.firstOrNull { it.isCurrent }
        }
        if (startState != null) {
            paths.add(Path(startState, 0))
        }

        while (paths.isNotEmpty()) {
            val nextPaths = mutableListOf<Path>()

            for (path in paths) {
                if (path.inputIndex == input.length && path.currentState.finite) {
                    return true
                }

                if (path.inputIndex == input.length) continue

                val currentChar = input[path.inputIndex]
                val possibleTransitions = transitions.filter {
                    it.startState == path.currentState.index && (it.name.isEmpty() || it.name.first() == currentChar)
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    nextPaths.add(Path(nextState, path.inputIndex + 1))
                }
            }

            paths = nextPaths
        }

        return false
    }

    override fun exportTransitionsToJFF(builder: StringBuilder) {
        for (transition in transitions) {
            builder.appendLine("        <transition>")
            builder.appendLine("            <from>${transition.startState}</from>")
            builder.appendLine("            <to>${transition.endState}</to>")
            builder.appendLine("            <read>${escapeXml(transition.name)}</read>")
            transition.controlPoint?.let { cp ->
                builder.appendLine("            <controlpoint>")
                builder.appendLine("                <x>${formatFloat(cp.x)}</x>")
                builder.appendLine("                <y>${formatFloat(cp.y)}</y>")
                builder.appendLine("            </controlpoint>")
            }
            builder.appendLine("        </transition>")
        }
    }

    private fun getListOfAppropriateTransitions(startState: State): List<Transition> {
        return transitions.filter {
            it.startState == startState.index && input.startsWith(it.name)
        }
    }
}
