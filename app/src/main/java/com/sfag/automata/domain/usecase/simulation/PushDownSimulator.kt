package com.sfag.automata.domain.usecase.simulation

import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.PushDownMachine
import com.sfag.automata.domain.model.PushDownSimulationState
import com.sfag.automata.domain.model.SimulationState
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.automata.presentation.model.AcceptanceCriteria

/**
 * Simulation engine for Pushdown Automata (PDA).
 */
class PushDownSimulator(
    private val acceptanceCriteria: AcceptanceCriteria = AcceptanceCriteria.BY_FINITE_STATE
) : SimulationEngine {

    override fun initialize(machine: Machine, input: String): SimulationState {
        val pdaMachine = machine as? PushDownMachine
        val initialState = machine.states.firstOrNull { it.initial }
        val initialStack = pdaMachine?.symbolStack?.toList() ?: listOf('Z')

        return PushDownSimulationState(
            currentStateId = initialState?.index,
            inputPosition = 0,
            isAccepted = null,
            isFinished = false,
            step = 0,
            remainingInput = input,
            originalInput = input,
            stack = initialStack
        )
    }

    override fun step(machine: Machine, state: SimulationState): Pair<SimulationState, Transition?> {
        require(state is PushDownSimulationState) { "Expected PushDownSimulationState" }

        if (state.currentStateId == null) {
            return state.copy(isFinished = true, isAccepted = false) to null
        }

        val currentState = machine.getStateByIndex(state.currentStateId)
        val availableTransitions = getAvailableTransitions(machine, state)

        if (availableTransitions.isEmpty()) {
            val accepted = checkAcceptance(state, currentState.finite)
            return state.copy(isFinished = true, isAccepted = accepted) to null
        }

        // Find best transition using look-ahead
        val bestTransition = availableTransitions.firstOrNull { transition ->
            val tempStack = applyStackOperations(state.stack, transition as? PushDownTransition)
                ?: return@firstOrNull false
            val nextInput = state.remainingInput.removePrefix(transition.name)
            val tempState = PushDownSimulationState(
                currentStateId = transition.endState,
                inputPosition = state.inputPosition + transition.name.length,
                isAccepted = null,
                isFinished = false,
                step = state.step,
                remainingInput = nextInput,
                originalInput = state.originalInput,
                stack = tempStack
            )
            canReachFinalState(machine, tempState)
        } ?: availableTransitions.first()

        // Apply the transition
        val pdaTransition = bestTransition as? PushDownTransition
        val newStack = applyStackOperations(state.stack, pdaTransition) ?: state.stack
        val newRemainingInput = state.remainingInput.removePrefix(bestTransition.name)
        val nextStateObj = machine.getStateByIndex(bestTransition.endState)
        val consumed = bestTransition.name.length

        val newState = PushDownSimulationState(
            currentStateId = bestTransition.endState,
            inputPosition = state.inputPosition + consumed,
            isAccepted = if (newRemainingInput.isEmpty()) checkAcceptance(
                state.copy(stack = newStack, remainingInput = newRemainingInput),
                nextStateObj.finite
            ) else null,
            isFinished = newRemainingInput.isEmpty(),
            step = state.step + 1,
            remainingInput = newRemainingInput,
            originalInput = state.originalInput,
            stack = newStack
        )

        return newState to bestTransition
    }

    override fun canReachFinalState(machine: Machine, state: SimulationState): Boolean {
        require(state is PushDownSimulationState) { "Expected PushDownSimulationState" }

        data class Path(val stateId: Int, val inputIndex: Int, val stack: List<Char>)

        val startStateId = state.currentStateId ?: return false
        var paths = mutableListOf(Path(startStateId, 0, state.stack))
        val input = state.remainingInput

        while (paths.isNotEmpty()) {
            val nextPaths = mutableListOf<Path>()

            for (path in paths) {
                val currentState = machine.getStateByIndex(path.stateId)

                // Check acceptance
                val accepted = when (acceptanceCriteria) {
                    AcceptanceCriteria.BY_FINITE_STATE ->
                        path.inputIndex == input.length && currentState.finite
                    AcceptanceCriteria.BY_INITIAL_STACK ->
                        path.inputIndex == input.length && path.stack == listOf('Z')
                }
                if (accepted) return true

                if (path.inputIndex > input.length) continue

                val currentChar = input.getOrNull(path.inputIndex)
                val possibleTransitions = machine.transitions.filter { t ->
                    t.startState == path.stateId &&
                            (t.name.isEmpty() || t.name.firstOrNull() == currentChar)
                }

                for (transition in possibleTransitions) {
                    val pdaTransition = transition as? PushDownTransition
                    val newStack = applyStackOperations(path.stack, pdaTransition) ?: continue

                    val consumed = if (transition.name.isEmpty()) 0 else 1
                    nextPaths.add(Path(transition.endState, path.inputIndex + consumed, newStack))
                }
            }

            paths = nextPaths
        }

        return false
    }

    override fun getAvailableTransitions(machine: Machine, state: SimulationState): List<Transition> {
        require(state is PushDownSimulationState) { "Expected PushDownSimulationState" }
        val currentStateId = state.currentStateId ?: return emptyList()

        return machine.transitions.filter { transition ->
            transition.startState == currentStateId &&
                    state.remainingInput.startsWith(transition.name) &&
                    canApplyStackOperations(state.stack, transition as? PushDownTransition)
        }
    }

    override fun reset(machine: Machine, state: SimulationState): SimulationState {
        require(state is PushDownSimulationState) { "Expected PushDownSimulationState" }
        return initialize(machine, state.originalInput)
    }

    private fun checkAcceptance(state: PushDownSimulationState, isFinalState: Boolean): Boolean {
        return when (acceptanceCriteria) {
            AcceptanceCriteria.BY_FINITE_STATE ->
                state.remainingInput.isEmpty() && isFinalState
            AcceptanceCriteria.BY_INITIAL_STACK ->
                state.remainingInput.isEmpty() && state.stack == listOf('Z')
        }
    }

    private fun canApplyStackOperations(stack: List<Char>, transition: PushDownTransition?): Boolean {
        if (transition == null) return true
        if (transition.pop.isEmpty()) return true
        return stack.isNotEmpty() && stack.last() == transition.pop.first()
    }

    private fun applyStackOperations(stack: List<Char>, transition: PushDownTransition?): List<Char>? {
        if (transition == null) return stack

        val newStack = stack.toMutableList()

        // POP operation
        if (transition.pop.isNotEmpty()) {
            val expectedTop = transition.pop.first()
            if (newStack.isEmpty() || newStack.last() != expectedTop) return null
            newStack.removeAt(newStack.lastIndex)
        }

        // PUSH operation (reversed order so last char is on top)
        if (transition.push.isNotEmpty()) {
            transition.push.reversed().forEach { symbol ->
                newStack.add(symbol)
            }
        }

        return newStack
    }
}
