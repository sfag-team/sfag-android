package com.sfag.automata.domain.usecase.simulation

import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.TuringMachine
import com.sfag.automata.domain.model.SimulationState
import com.sfag.automata.domain.model.TuringSimulationState
import com.sfag.automata.domain.model.transition.TapeDirection
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.automata.domain.model.transition.TuringTransition

/**
 * Simulation engine for Turing Machines.
 */
class TuringSimulator : SimulationEngine {

    companion object {
        private const val TAPE_PADDING = 10 // Extra blank cells on each side
    }

    override fun initialize(machine: Machine, input: String): SimulationState {
        val turingMachine = machine as? TuringMachine
        val blankSymbol = turingMachine?.blankSymbol ?: '_'
        val initialState = machine.states.firstOrNull { it.initial }

        // Create tape with padding on both sides
        val tape = mutableListOf<Char>()
        repeat(TAPE_PADDING) { tape.add(blankSymbol) }
        tape.addAll(input.toList())
        repeat(TAPE_PADDING) { tape.add(blankSymbol) }

        return TuringSimulationState(
            currentStateId = initialState?.index,
            inputPosition = 0,
            isAccepted = null,
            isFinished = false,
            step = 0,
            tape = tape,
            headPosition = TAPE_PADDING, // Start at beginning of input
            blankSymbol = blankSymbol
        )
    }

    override fun step(machine: Machine, state: SimulationState): Pair<SimulationState, Transition?> {
        require(state is TuringSimulationState) { "Expected TuringSimulationState" }

        if (state.currentStateId == null) {
            return state.copy(isFinished = true, isAccepted = false) to null
        }

        val currentState = machine.getStateByIndex(state.currentStateId)

        // Check if in accepting state
        if (currentState.finite) {
            return state.copy(isFinished = true, isAccepted = true) to null
        }

        val availableTransitions = getAvailableTransitions(machine, state)

        if (availableTransitions.isEmpty()) {
            // Halted in non-accepting state
            return state.copy(isFinished = true, isAccepted = false) to null
        }

        // For deterministic TM, take the first (only) transition
        val transition = availableTransitions.first() as TuringTransition

        // Apply transition
        val newTape = state.tape.toMutableList()
        newTape[state.headPosition] = transition.writeSymbol

        var newHeadPosition = when (transition.direction) {
            TapeDirection.LEFT -> state.headPosition - 1
            TapeDirection.RIGHT -> state.headPosition + 1
            TapeDirection.STAY -> state.headPosition
        }

        // Expand tape if necessary
        if (newHeadPosition < 0) {
            newTape.add(0, state.blankSymbol)
            newHeadPosition = 0
        } else if (newHeadPosition >= newTape.size) {
            newTape.add(state.blankSymbol)
        }

        val nextState = machine.getStateByIndex(transition.endState)
        val newState = TuringSimulationState(
            currentStateId = transition.endState,
            inputPosition = state.inputPosition + 1,
            isAccepted = if (nextState.finite) true else null,
            isFinished = nextState.finite,
            step = state.step + 1,
            tape = newTape,
            headPosition = newHeadPosition,
            blankSymbol = state.blankSymbol
        )

        return newState to transition
    }

    override fun canReachFinalState(machine: Machine, state: SimulationState): Boolean {
        // For Turing machines, this is undecidable in general
        // We can only do bounded simulation
        require(state is TuringSimulationState) { "Expected TuringSimulationState" }

        var currentState = state
        var steps = 0
        val maxSteps = 1000 // Prevent infinite loops

        while (steps < maxSteps) {
            if (currentState.isFinished) {
                return currentState.isAccepted == true
            }

            val (nextState, _) = step(machine, currentState)
            if (nextState == currentState) break // No progress
            currentState = nextState as TuringSimulationState
            steps++
        }

        return false // Couldn't determine
    }

    override fun getAvailableTransitions(machine: Machine, state: SimulationState): List<Transition> {
        require(state is TuringSimulationState) { "Expected TuringSimulationState" }
        val currentStateId = state.currentStateId ?: return emptyList()
        val currentSymbol = state.tape.getOrNull(state.headPosition) ?: state.blankSymbol

        return machine.transitions.filter { transition ->
            transition.startState == currentStateId &&
                    (transition as? TuringTransition)?.name?.firstOrNull() == currentSymbol
        }
    }

    override fun reset(machine: Machine, state: SimulationState): SimulationState {
        require(state is TuringSimulationState) { "Expected TuringSimulationState" }
        // Extract original input from tape (remove padding and blank symbols)
        val originalInput = state.tape
            .dropWhile { it == state.blankSymbol }
            .dropLastWhile { it == state.blankSymbol }
            .joinToString("")
        return initialize(machine, originalInput)
    }
}
