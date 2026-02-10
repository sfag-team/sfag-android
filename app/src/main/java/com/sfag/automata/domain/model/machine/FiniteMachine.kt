package com.sfag.automata.domain.model.machine

import com.sfag.automata.domain.model.simulation.SimulationResult
import com.sfag.automata.domain.model.simulation.TransitionData
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.automata.domain.model.tree.Branch
import com.sfag.automata.domain.model.NODE_RADIUS
import com.sfag.shared.util.Symbols
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
    // Track multiple current states for NFA simulation
    val currentStates: MutableSet<Int> = mutableSetOf()

    // currentState now derives from currentStates for compatibility
    override var currentState: Int?
        get() = currentStates.firstOrNull()
        set(value) {
            currentStates.clear()
            value?.let { currentStates.add(it) }
        }

    /**
     * Calculate next simulation step - shows all paths (JFLAP behavior).
     * Works for both DFA and NFA.
     */
    override fun calculateNextStep(): SimulationResult {
        // Initialize current states if empty
        if (currentStates.isEmpty()) {
            val initialState = states.firstOrNull { it.initial }
            if (initialState != null) {
                currentStates.add(initialState.index)
                initialState.isCurrent = true
            }
        }

        if (currentStates.isEmpty()) {
            return SimulationResult.Ended(null)
        }

        return calculateAllPathsStep()
    }

    /**
     * Process all transitions from all current states.
     * Groups transitions by input length to handle different-length transitions correctly.
     */
    private fun calculateAllPathsStep(): SimulationResult {
        // Collect all possible transitions from all current states
        val allTransitions = mutableListOf<Pair<State, Transition>>()
        for (stateIndex in currentStates) {
            val state = getStateByIndex(stateIndex)
            val possibleTransitions = getListOfAppropriateTransitions(state)
            for (transition in possibleTransitions) {
                allTransitions.add(state to transition)
            }
        }

        // No transitions available - check if any current state is accepting
        if (allTransitions.isEmpty()) {
            val anyAccepting = currentStates.any { stateIndex ->
                getStateByIndex(stateIndex).finite && input.isEmpty()
            }
            // Mark derivation tree: accepted paths green, rest rejected
            if (anyAccepting) {
                val acceptedIds = derivationTree.getActiveNodes()
                    .filter { node -> states.any { it.name == node.stateName && it.finite } }
                    .map { it.id }.toSet()
                derivationTree.markAcceptedPaths(acceptedIds)
            }
            derivationTree.markRemainingAsRejected()
            return SimulationResult.Ended(anyAccepting)
        }

        // Group transitions by input length - only process shortest ones this step
        // (longer transitions will be processed in subsequent steps)
        val minInputLength = allTransitions.minOf { it.second.name.length }
        val transitionsToProcess = allTransitions.filter { it.second.name.length == minInputLength }

        // Build transition data for animation
        val transitionDataList = transitionsToProcess.map { (startState, transition) ->
            val endState = getStateByIndex(transition.endState)
            TransitionData(
                startPosition = startState.position,
                endPosition = endState.position,
                radius = NODE_RADIUS
            )
        }

        // Calculate next states (all reachable states from processed transitions)
        val nextStates = transitionsToProcess.map { it.second.endState }.toSet()

        // Expand derivation tree before consuming input
        expandDerivationTree()

        // Consume input based on the transitions being processed
        consumeInput(minInputLength)

        return SimulationResult.MultipleTransitions(
            transitions = transitionDataList,
            onAllComplete = {
                // Clear old current states
                for (stateIndex in currentStates) {
                    getStateByIndexOrNull(stateIndex)?.isCurrent = false
                }
                currentStates.clear()

                // Set new current states
                for (stateIndex in nextStates) {
                    currentStates.add(stateIndex)
                    getStateByIndexOrNull(stateIndex)?.isCurrent = true
                }
            }
        )
    }

    private fun consumeInput(length: Int) {
        if (length > 0) {
            val newInputValue = input.drop(length).toString()
            input.clear()
            input.append(newInputValue)
            if (imuInput.isNotEmpty()) {
                imuInput.delete(0, minOf(length, imuInput.length))
            }
        }
    }

    /**
     * Checks if input was fully consumed and machine is in accepting state.
     * Returns true if ANY current state is accepting.
     */
    fun isAccepted(): Boolean? {
        if (currentStates.isEmpty()) return null
        val anyAccepting = currentStates.any { stateIndex ->
            getStateByIndexOrNull(stateIndex)?.finite == true
        }
        return if (input.isEmpty() && anyAccepting) true else null
    }

    override fun resetMachineState() {
        // For NFA: clear isCurrent flag on all non-initial states
        // (setInitialStateAsCurrent only clears one state via currentState getter)
        val initialStateIndex = states.firstOrNull { it.initial }?.index
        for (state in states) {
            if (state.index != initialStateIndex) {
                state.isCurrent = false
            }
        }
        // Reset currentStates to only contain initial state
        currentStates.clear()
        initialStateIndex?.let { currentStates.add(it) }
    }

    override fun addNewState(state: State) {
        if (state.initial && currentStates.isEmpty()) {
            currentStates.add(state.index)
            state.isCurrent = true
        }
        states.add(state)
    }

    override fun expandDerivationTree() {
        val active = derivationTree.getActiveNodes()
        if (active.isEmpty()) return

        val branches = mutableMapOf<Int, List<Branch>>()
        for (node in active) {
            val state = states.firstOrNull { it.name == node.stateName }
            if (state == null) {
                branches[node.id] = emptyList()
                continue
            }
            val possibleTransitions = transitions.filter {
                it.startState == state.index && input.startsWith(it.name)
            }
            if (possibleTransitions.isEmpty()) {
                branches[node.id] = emptyList()
                continue
            }
            val minLen = possibleTransitions.minOf { it.name.length }
            val filtered = possibleTransitions.filter { it.name.length == minLen }
            branches[node.id] = filtered.map { t ->
                val endState = getStateByIndex(t.endState)
                Branch(endState.name)
            }
        }
        derivationTree.expandActive(branches)
    }

    override fun getMathFormatData(): MachineFormatData {
        val transitionDescriptions = transitions.map { t ->
            val fromState = states.find { it.index == t.startState }?.name ?: "?"
            val toState = states.find { it.index == t.endState }?.name ?: "?"
            val readSymbol = t.name.ifEmpty { Symbols.EPSILON }
            "${Symbols.DELTA}($fromState, $readSymbol) = $toState"
        }

        return MachineFormatData(
            stateNames = states.map { it.name },
            inputAlphabet = transitions.mapNotNull { it.name.firstOrNull() }.toSet(),
            initialStateName = states.firstOrNull { it.initial }?.name ?: "q0",
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
