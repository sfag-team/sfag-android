package com.sfag.automata.domain.model.machine

import com.sfag.automata.domain.model.simulation.SimulationResult
import com.sfag.automata.domain.model.simulation.TransitionData
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.shared.util.Symbols
import com.sfag.shared.util.XmlUtils.formatFloat
import com.sfag.shared.util.XmlUtils.xmlTag

@Suppress("UNCHECKED_CAST")
class PushDownMachine(
    name: String = "",
    version: Int = 1,
    states: MutableList<State> = mutableListOf(),
    transitions: MutableList<PushDownTransition> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    val symbolStack: MutableList<Char> = mutableListOf('Z')
) : Machine(
    name, version,
    machineType = MachineType.Pushdown, states, transitions as MutableList<Transition>, savedInputs = savedInputs
) {
    override var currentState: Int? = null
    var acceptanceCriteria = AcceptanceCriteria.BY_FINAL_STATE

    override fun calculateNextStep(): SimulationResult {
        if (!ensureCurrentState()) return SimulationResult.Ended(null)
        val currentStateIndex = currentState!!

        val startState = getStateByIndex(currentStateIndex)
        val conditionDone = if(acceptanceCriteria == AcceptanceCriteria.BY_FINAL_STATE) startState.final else symbolStack.size==1
        val possibleTransitions = getListOfAppropriateTransitions(startState)
        if (possibleTransitions.isEmpty()) {
            return SimulationResult.Ended(conditionDone)
        }

        var validTransition: Transition? = possibleTransitions.firstOrNull { transition ->
            val nextInput = input.removePrefix(transition.name)
            val tempStack = symbolStack.toMutableList()

            if (!applyStackOperation(transition as PushDownTransition, tempStack)) return@firstOrNull false

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
        applyStackOperation(validTransition as PushDownTransition, symbolStack)

        val consumed = validTransition.name.length
        if (consumed > 0 && imuInput.isNotEmpty()) {
            imuInput.delete(0, minOf(consumed, imuInput.length))
        }
        return SimulationResult.Step(
            transitions = listOf(TransitionData(startStateIndex = startState.index, endStateIndex = endState.index)),
            onAllComplete = {
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
        if (input.isNotEmpty()) return null
        val currentIdx = currentState ?: return null
        val state = getStateByIndexOrNull(currentIdx) ?: return null
        return when (acceptanceCriteria) {
            AcceptanceCriteria.BY_FINAL_STATE -> if (state.final) true else null
            AcceptanceCriteria.BY_INITIAL_STACK -> if (symbolStack.size == 1) true else null
        }
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
        val existing = transitions.filterIsInstance<PushDownTransition>()
        transitions.add(
            if (pop != "") {
                if (existing.any {
                    it.name == name && it.startState == startState.index &&
                        it.endState == endState.index && it.pop == pop && it.push == ""
                }) return
                PushDownTransition(name, startState.index, endState.index, pop = pop, push = "")
            } else if (checkState != "") {
                if (existing.any {
                    it.name == name && it.startState == startState.index &&
                        it.endState == endState.index && it.pop == checkState && it.push == push + checkState
                }) return
                PushDownTransition(name, startState.index, endState.index, pop = checkState, push = push + checkState)
            } else {
                PushDownTransition(name, startState.index, endState.index, pop = "", push = push)
            }
        )
    }

    override fun resetMachineState() {
        symbolStack.clear()
        symbolStack.add('Z')
    }

    override fun expandDerivationTree() {
        // PDA: no incremental derivation tree
    }

    override fun getMathFormatData(): MachineFormatData {
        val pdaTransitions = transitions.filterIsInstance<PushDownTransition>()

        val transitionDescriptions = pdaTransitions.map { t ->
            val fromState = getStateByIndexOrNull(t.startState)?.name ?: "?"
            val toState = getStateByIndexOrNull(t.endState)?.name ?: "?"
            val readSymbol = t.name.ifEmpty { Symbols.EPSILON }
            val popSymbol = t.pop.ifEmpty { Symbols.EPSILON }
            val pushSymbol = t.push.ifEmpty { Symbols.EPSILON }
            "${Symbols.DELTA}($fromState, $readSymbol, $popSymbol) = ($toState, $pushSymbol)"
        }

        val stackAlphabetSet = pdaTransitions
            .flatMap { (it.pop + it.push).toCharArray().toList() }
            .toSet()
            .plus('Z') // Always include initial stack symbol

        return MachineFormatData(
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
    private fun applyStackOperation(transition: PushDownTransition, stack: MutableList<Char>): Boolean {
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

                    if (!applyStackOperation(transition as PushDownTransition, newStack)) continue

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
        for (transition in transitions.filterIsInstance<PushDownTransition>()) {
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
                    (transition !is PushDownTransition || transition.pop.isEmpty() ||
                            (symbolStack.isNotEmpty() && symbolStack.last() == transition.pop.first()))
        }
    }
}
