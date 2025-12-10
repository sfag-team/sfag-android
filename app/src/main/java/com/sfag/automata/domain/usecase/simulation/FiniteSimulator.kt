package com.sfag.automata.domain.usecase.simulation

import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.FiniteSimulationState
import com.sfag.automata.domain.model.SimulationState
import com.sfag.automata.domain.model.transition.Transition

/**
 * Simulation engine for Finite Automata (DFA/NFA).
 */
class FiniteSimulator : SimulationEngine {

    override fun initialize(machine: Machine, input: String): SimulationState {
        val initialState = machine.states.firstOrNull { it.initial }
        return FiniteSimulationState(
            currentStateId = initialState?.index,
            inputPosition = 0,
            isAccepted = null,
            isFinished = false,
            step = 0,
            remainingInput = input,
            originalInput = input
        )
    }

    override fun step(machine: Machine, state: SimulationState): Pair<SimulationState, Transition?> {
        require(state is FiniteSimulationState) { "Expected FiniteSimulationState" }

        if (state.currentStateId == null) {
            return state.copy(isFinished = true, isAccepted = false) to null
        }

        val currentState = machine.getStateByIndex(state.currentStateId)
        val availableTransitions = getAvailableTransitions(machine, state)

        if (availableTransitions.isEmpty()) {
            val accepted = state.remainingInput.isEmpty() && currentState.finite
            return state.copy(isFinished = true, isAccepted = accepted) to null
        }

        // Try to find a transition that leads to acceptance (look-ahead for NFA)
        val bestTransition = availableTransitions.firstOrNull { transition ->
            val nextInput = state.remainingInput.removePrefix(transition.name)
            val nextStateId = transition.endState
            val tempState = state.copy(
                currentStateId = nextStateId,
                remainingInput = nextInput,
                inputPosition = state.inputPosition + transition.name.length
            )
            canReachFinalState(machine, tempState)
        } ?: availableTransitions.first()

        val nextState = machine.getStateByIndex(bestTransition.endState)
        val newRemainingInput = state.remainingInput.removePrefix(bestTransition.name)
        val consumed = bestTransition.name.length

        val newState = FiniteSimulationState(
            currentStateId = nextState.index,
            inputPosition = state.inputPosition + consumed,
            isAccepted = if (newRemainingInput.isEmpty() && nextState.finite) true else null,
            isFinished = newRemainingInput.isEmpty() && getAvailableTransitionsForInput(machine, nextState.index, "").isEmpty(),
            step = state.step + 1,
            remainingInput = newRemainingInput,
            originalInput = state.originalInput
        )

        return newState to bestTransition
    }

    override fun canReachFinalState(machine: Machine, state: SimulationState): Boolean {
        require(state is FiniteSimulationState) { "Expected FiniteSimulationState" }

        data class Path(val stateId: Int, val inputIndex: Int)

        val startStateId = state.currentStateId ?: return false
        var paths = mutableListOf(Path(startStateId, 0))
        val input = state.remainingInput

        while (paths.isNotEmpty()) {
            val nextPaths = mutableListOf<Path>()

            for (path in paths) {
                val currentState = machine.getStateByIndex(path.stateId)

                if (path.inputIndex == input.length && currentState.finite) {
                    return true
                }

                if (path.inputIndex >= input.length) continue

                val currentChar = input[path.inputIndex]
                val possibleTransitions = machine.transitions.filter {
                    it.startState == path.stateId &&
                            (it.name.isEmpty() || it.name.firstOrNull() == currentChar)
                }

                for (transition in possibleTransitions) {
                    val consumed = if (transition.name.isEmpty()) 0 else 1
                    nextPaths.add(Path(transition.endState, path.inputIndex + consumed))
                }
            }

            paths = nextPaths
        }

        return false
    }

    override fun getAvailableTransitions(machine: Machine, state: SimulationState): List<Transition> {
        require(state is FiniteSimulationState) { "Expected FiniteSimulationState" }
        val currentStateId = state.currentStateId ?: return emptyList()
        return getAvailableTransitionsForInput(machine, currentStateId, state.remainingInput)
    }

    private fun getAvailableTransitionsForInput(machine: Machine, stateId: Int, input: String): List<Transition> {
        return machine.transitions.filter { transition ->
            transition.startState == stateId && input.startsWith(transition.name)
        }
    }

    override fun reset(machine: Machine, state: SimulationState): SimulationState {
        require(state is FiniteSimulationState) { "Expected FiniteSimulationState" }
        return initialize(machine, state.originalInput)
    }
}
