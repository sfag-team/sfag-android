package com.sfag.automata.model.machine

import com.sfag.automata.model.simulation.SimulationOutcome
import com.sfag.automata.model.simulation.SimulationStep
import com.sfag.automata.model.simulation.TransitionData

import com.sfag.automata.model.transition.PushdownTransition
import com.sfag.automata.model.transition.Transition
import com.sfag.shared.Symbols
import com.sfag.shared.util.XmlUtils.xmlTag

enum class AcceptanceCriteria(val text: String) {
    BY_FINAL_STATE("the final state"),
    BY_INITIAL_STACK("the initial stack (\"Z\")")
}

class PushdownMachine(
    name: String = "",
    version: Int = 1,
    states: MutableList<State> = mutableListOf(),
    transitions: MutableList<Transition> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    val symbolStack: MutableList<Char> = mutableListOf('Z')
) : Machine(
    name, version,
    machineType = MachineType.Pushdown, states, transitions, savedInputs = savedInputs
) {
    override var currentState: Int? = null
    var acceptanceCriteria = AcceptanceCriteria.BY_FINAL_STATE

    override fun calculateNextStep(): SimulationStep {
        if (!ensureCurrentState()) return SimulationStep.Ended(SimulationOutcome.ACTIVE)
        val currentStateIndex = currentState!!

        val startState = getStateByIndex(currentStateIndex)
        val conditionDone = if(acceptanceCriteria == AcceptanceCriteria.BY_FINAL_STATE) startState.final else symbolStack.size==1
        val possibleTransitions = getListOfAppropriateTransitions(startState)
        if (possibleTransitions.isEmpty()) {
            return SimulationStep.Ended(if (conditionDone) SimulationOutcome.ACCEPTED else SimulationOutcome.REJECTED)
        }

        var validTransition: Transition? = possibleTransitions.firstOrNull { transition ->
            val nextInput = input.removePrefix(transition.name)
            val tempStack = symbolStack.toMutableList()

            if (!applyStackOperation(transition as PushdownTransition, tempStack)) return@firstOrNull false

            // check if we can reach final state
            val nextState = getStateByIndex(transition.endState)
            val previousCurrent = currentState ?: return@firstOrNull false

            states.forEach { it.isCurrent = false }
            nextState.isCurrent = true
            currentState = nextState.index

            val result = when (acceptanceCriteria) {
                AcceptanceCriteria.BY_FINAL_STATE -> canReachFinalStatePDA(StringBuilder(nextInput), false, tempStack)
                AcceptanceCriteria.BY_INITIAL_STACK -> canReachInitialStackPDA(StringBuilder(nextInput), false, tempStack)
            }

            nextState.isCurrent = false
            getStateByIndexOrNull(previousCurrent)?.isCurrent = true
            currentState = previousCurrent

            result
        }

        // fallback transition
        if (validTransition == null) {
            validTransition = possibleTransitions.first()
        }

        val endState = getStateByIndex(validTransition.endState)
        input.delete(0, validTransition.name.length)

        // Actual push/pop on symbolStack
        applyStackOperation(validTransition as PushdownTransition, symbolStack)

        val consumed = validTransition.name.length
        if (consumed > 0 && imuInput.isNotEmpty()) {
            imuInput.delete(0, minOf(consumed, imuInput.length))
        }
        return SimulationStep.Step(
            transitions = listOf(TransitionData(startStateIndex = startState.index, endStateIndex = endState.index)),
            onAllComplete = {
                startState.isCurrent = false
                endState.isCurrent = true
                currentState = endState.index
            }
        )
    }

    override fun addNewState(state: State) {
        if (state.initial && currentState == null) {
            currentState = state.index
            state.isCurrent = true
        }
        states.add(state)
    }

    fun addNewTransition(
        name: String,
        startState: State,
        endState: State,
        pop: String,
        checkState: String,
        push: String
    ) {
        val existing = transitions.filterIsInstance<PushdownTransition>()
        transitions.add(
            if (pop != "") {
                if (existing.any {
                    it.name == name && it.startState == startState.index &&
                        it.endState == endState.index && it.pop == pop && it.push == ""
                }) return
                PushdownTransition(name, startState.index, endState.index, pop = pop, push = "")
            } else if (checkState != "") {
                if (existing.any {
                    it.name == name && it.startState == startState.index &&
                        it.endState == endState.index && it.pop == checkState && it.push == push + checkState
                }) return
                PushdownTransition(name, startState.index, endState.index, pop = checkState, push = push + checkState)
            } else {
                PushdownTransition(name, startState.index, endState.index, pop = "", push = push)
            }
        )
    }

    override fun resetMachineState() {
        symbolStack.clear()
        symbolStack.add('Z')
    }

    override fun expandExplorationTree() {
        // PDA: no incremental derivation tree
    }

    override fun getFormalDefinition(): MachineFormalDefinition {
        val pdaTransitions = transitions.filterIsInstance<PushdownTransition>()

        val transitionDescriptions = pdaTransitions.map { t ->
            val fromState = getStateByIndexOrNull(t.startState)?.name ?: "?"
            val toState = getStateByIndexOrNull(t.endState)?.name ?: "?"
            val readSymbol = t.name.ifEmpty { "${Symbols.EPSILON}" }
            val popSymbol = t.pop.ifEmpty { "${Symbols.EPSILON}" }
            val pushSymbol = t.push.ifEmpty { "${Symbols.EPSILON}" }
            "${Symbols.DELTA}($fromState, $readSymbol, $popSymbol) = ($toState, $pushSymbol)"
        }

        val stackAlphabetSet = pdaTransitions
            .flatMap { (it.pop + it.push).toCharArray().toList() }
            .toSet()
            .plus('Z') // Always include initial stack symbol

        return MachineFormalDefinition(
            stateNames = states.map { it.name },
            inputAlphabet = transitions.mapNotNull { it.name.firstOrNull() }.toSet(),
            initialStateName = states.firstOrNull { it.initial }?.name ?: "q0",
            finalStateNames = states.filter { it.final }.map { it.name },
            transitionDescriptions = transitionDescriptions,
            machineType = machineType,
            stackAlphabet = stackAlphabetSet,
            initialStackSymbol = 'Z'
        )
    }

    override fun canReachFinalState(input: StringBuilder, fromInit: Boolean): Boolean {
        return canReachFinalStatePDA(input, fromInit = fromInit, initialStack = if(fromInit) listOf('Z') else symbolStack)
    }

    /**
     * Apply pop/push stack operations for a PDA transition.
     * Returns false if the pop cannot be performed (stack empty or top mismatch).
     */
    private fun applyStackOperation(transition: PushdownTransition, stack: MutableList<Char>): Boolean {
        if (transition.pop.isNotEmpty()) {
            if (stack.isEmpty() || stack.last() != transition.pop.first()) return false
            stack.removeAt(stack.lastIndex)
        }
        if (transition.push.isNotEmpty()) {
            transition.push.reversed().forEach { stack.add(it) }
        }
        return true
    }

    private fun canReachPDA(
        input: StringBuilder,
        fromInit: Boolean,
        stack: List<Char>,
        isAccepted: (State, List<Char>) -> Boolean
    ): Boolean {
        data class BfsPath(
            val currentState: State,
            val inputIndex: Int,
            val symbolStack: List<Char>
        )

        var startState = states.firstOrNull { if (!fromInit) it.isCurrent else it.initial }
        if (startState == null) {
            setInitialStateAsCurrent()
            startState = states.firstOrNull { it.isCurrent }
        }
        if (startState == null) return false

        var paths = mutableListOf(BfsPath(startState, 0, stack))

        while (paths.isNotEmpty()) {
            val nextPaths = mutableListOf<BfsPath>()

            for (path in paths) {
                if (path.inputIndex == input.length && isAccepted(path.currentState, path.symbolStack)) {
                    return true
                }

                val remainingInput = input.substring(path.inputIndex)
                val possibleTransitions = transitions.filter {
                    it.startState == path.currentState.index &&
                            (it.name.isEmpty() || remainingInput.startsWith(it.name))
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    val newStack = path.symbolStack.toMutableList()

                    if (!applyStackOperation(transition as PushdownTransition, newStack)) continue

                    nextPaths.add(BfsPath(nextState, path.inputIndex + transition.name.length, newStack))
                }
            }

            paths = nextPaths
        }

        return false
    }

    private fun canReachFinalStatePDA(input: StringBuilder, fromInit: Boolean, initialStack: List<Char>) =
        canReachPDA(input, fromInit, initialStack) { state, _ -> state.final }

    fun canReachInitialStackPDA(input: StringBuilder, fromInit: Boolean = false, symbolStack: List<Char>) =
        canReachPDA(input, fromInit, symbolStack) { _, s -> s == listOf('Z') }

    override fun exportTransitionsToJFF(builder: StringBuilder) {
        for (transition in transitions.filterIsInstance<PushdownTransition>()) {
            builder.appendLine("        <transition>")
            builder.appendLine("            <from>${transition.startState}</from>")
            builder.appendLine("            <to>${transition.endState}</to>")
            builder.appendLine("            ${xmlTag("read", transition.name)}")
            builder.appendLine("            ${xmlTag("pop", transition.pop)}")
            builder.appendLine("            ${xmlTag("push", transition.push)}")
            builder.appendLine("        </transition>")
        }
    }

    private fun getListOfAppropriateTransitions(startState: State): List<Transition> {
        return transitions.filter { transition ->
            transition.startState == startState.index &&
                    input.startsWith(transition.name) &&
                    (transition !is PushdownTransition || transition.pop.isEmpty() ||
                            (symbolStack.isNotEmpty() && symbolStack.last() == transition.pop.first()))
        }
    }
}
