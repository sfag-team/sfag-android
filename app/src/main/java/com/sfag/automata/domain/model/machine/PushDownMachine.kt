package com.sfag.automata.domain.model.machine

import com.sfag.automata.domain.model.simulation.SimulationResult
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.automata.domain.model.tree.TreeNode
import com.sfag.shared.util.XmlUtils.escapeXml
import com.sfag.shared.util.XmlUtils.formatFloat
import com.sfag.automata.presentation.model.AcceptanceCriteria


@Suppress("UNCHECKED_CAST")
class PushDownMachine(
    name: String,
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
    var acceptanceCriteria = AcceptanceCriteria.BY_FINITE_STATE

    override fun calculateNextStep(): SimulationResult {
        if (currentState == null) currentState = states.firstOrNull { it.initial }?.index
        val currentStateIndex = currentState
            ?: return SimulationResult.Ended(null)

        val startState = getStateByIndex(currentStateIndex)
        val conditionDone = if(acceptanceCriteria == AcceptanceCriteria.BY_FINITE_STATE) startState.finite else symbolStack.size==1
        val possibleTransitions = getListOfAppropriateTransitions(startState)
        if (possibleTransitions.isEmpty()) {
            return SimulationResult.Ended(conditionDone)
        }

        var validTransition: Transition? = possibleTransitions.firstOrNull { transition ->
            val nextInput = input.removePrefix(transition.name)
            val tempStack = symbolStack.toMutableList()

            if (transition is PushDownTransition) {
                // POP
                if (transition.pop.isNotEmpty()) {
                    val expectedTop = transition.pop.first()
                    if (tempStack.isEmpty() || tempStack.last() != expectedTop) return@firstOrNull false
                    tempStack.removeAt(tempStack.lastIndex)
                }

                // PUSH (in correct order: last symbol is top of stack)
                if (transition.push.isNotEmpty()) {
                    transition.push.reversed().forEach { symbol ->
                        tempStack.add(symbol)
                    }
                }
            }

            // check if we can reach final state
            val nextState = getStateByIndex(transition.endState)
            val previousCurrent = currentState ?: return@firstOrNull false

            states.forEach { it.isCurrent = false }
            nextState.isCurrent = true
            currentState = nextState.index

            val result = when (acceptanceCriteria) {
                AcceptanceCriteria.BY_FINITE_STATE -> canReachFinalStatePDA(StringBuilder(nextInput), false, tempStack)
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
        val newInputValue = input.removePrefix(validTransition.name).toString()
        input.clear()
        input.append(newInputValue)

        // Actual push/pop on symbolStack
        if (validTransition is PushDownTransition) {
            // POP
            if (validTransition.pop.isNotEmpty()) {
                symbolStack.removeAt(symbolStack.lastIndex)
            }

            // PUSH (in correct order)
            if (validTransition.push.isNotEmpty()) {
                validTransition.push.reversed().forEach { symbol ->
                    symbolStack.add(symbol)
                }
            }
        }
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
        if (input.isNotEmpty()) return null
        val currentIdx = currentState ?: return null
        val state = getStateByIndexOrNull(currentIdx) ?: return null
        return when (acceptanceCriteria) {
            AcceptanceCriteria.BY_FINITE_STATE -> if (state.finite) true else null
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

    override fun resetMachineState() {
        symbolStack.clear()
        symbolStack.add('Z')
    }

    override fun getDerivationTreeElements(): List<List<TreeNode>> {
        val allPaths = mutableListOf<Pair<List<String?>, List<Char>>>()

        data class Path(
            val history: List<String?>,
            val currentState: State?,
            val inputIndex: Int,
            val symbolStack: List<Char>,
            val alive: Boolean
        )

        val startStates = states.filter { it.initial }
        var paths = startStates.map {
            Path(listOf(null), it, 0, listOf('Z'), true)
        }.toMutableList()

        while (paths.any { it.alive }) {
            val nextPaths = mutableListOf<Path>()

            paths.forEach { path ->
                if (!path.alive) {
                    nextPaths.add(path.copy(history = path.history + null, currentState = null, alive = false))
                    return@forEach
                }

                val currentChar = imuInput.getOrNull(path.inputIndex)
                val currentStack = path.symbolStack.toMutableList()

                val possibleTransitions = transitions
                    .filter { it.startState == path.currentState?.index }
                    .filter { it.name.isEmpty() || it.name.firstOrNull() == currentChar }

                if (possibleTransitions.isEmpty()) {
                    allPaths.add(path.history + path.currentState?.name to path.symbolStack)
                    nextPaths.add(path.copy(history = path.history + null, currentState = null, alive = false))
                    return@forEach
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    val newStack = currentStack.toMutableList()

                    if (transition is PushDownTransition) {
                        if (transition.pop.isNotEmpty()) {
                            val expectedTop = transition.pop.first()
                            if (newStack.isEmpty() || newStack.last() != expectedTop) continue
                            newStack.removeAt(newStack.lastIndex)
                        }

                        if (transition.push.isNotEmpty()) {
                            transition.push.reversed().forEach { symbol ->
                                newStack.add(symbol)
                            }
                        }
                    }

                    val newInputIndex = if (transition.name.isEmpty()) path.inputIndex else path.inputIndex + 1

                    nextPaths.add(
                        Path(
                            history = path.history + path.currentState?.name,
                            currentState = nextState,
                            inputIndex = newInputIndex,
                            symbolStack = newStack,
                            alive = true
                        )
                    )
                }
            }

            paths = nextPaths
        }

        val acceptedPaths = allPaths.filter { (path, stack) ->
            val last = path.lastOrNull()
            when (acceptanceCriteria) {
                AcceptanceCriteria.BY_FINITE_STATE ->
                    last != null && states.any { it.name == last && it.finite }

                AcceptanceCriteria.BY_INITIAL_STACK ->
                    stack == listOf('Z')
            }
        }.map { it.first }

        val maxDepth = allPaths.maxOfOrNull { it.first.size } ?: 0
        val normalizedPaths = allPaths.map { (path, _) ->
            buildList {
                addAll(path)
                while (size < maxDepth) add(null)
            }
        }

        val tree = mutableListOf<List<TreeNode>>()

        for (level in 1 until maxDepth) {
            val nodeMap = mutableMapOf<String?, MutableList<Int>>()

            normalizedPaths.forEachIndexed { index, path ->
                val stateName = path[level]
                nodeMap.computeIfAbsent(stateName) { mutableListOf() }.add(index)
            }

            val levelNodes = nodeMap.map { (stateName, indices) ->
                val weight = indices.size.toFloat()
                val isAccepted = indices.any { acceptedPaths.contains(normalizedPaths[it]) }

                val isCurrent = stateName != null &&
                        states.firstOrNull { it.name == stateName }?.isCurrent == true &&
                        currentTreePosition == level

                TreeNode(
                    stateName = stateName,
                    weight = weight,
                    isAccepted = isAccepted,
                    isCurrent = isCurrent
                )
            }

            tree.add(levelNodes)
        }

        return tree
    }

    override fun getMathFormatData(): MachineFormatData {
        val pdaTransitions = transitions.filterIsInstance<PushDownTransition>()

        val transitionDescriptions = pdaTransitions.map { t ->
            val fromState = states.find { it.index == t.startState }?.name ?: "?"
            val toState = states.find { it.index == t.endState }?.name ?: "?"
            val readSymbol = t.name.ifEmpty { "ε" }
            val popSymbol = t.pop.ifEmpty { "ε" }
            val pushSymbol = t.push.ifEmpty { "ε" }
            "δ($fromState, $readSymbol, $popSymbol) = ($toState, $pushSymbol)"
        }

        val stackAlphabetSet = pdaTransitions
            .flatMap { (it.pop + it.push).toCharArray().toList() }
            .toSet()
            .plus('Z') // Always include initial stack symbol

        return MachineFormatData(
            stateNames = states.map { it.name },
            inputAlphabet = transitions.mapNotNull { it.name.firstOrNull() }.toSet(),
            initialStateName = states.firstOrNull { it.initial }?.name ?: "q₀",
            finalStateNames = states.filter { it.finite }.map { it.name },
            transitionDescriptions = transitionDescriptions,
            machineType = machineType,
            stackAlphabet = stackAlphabetSet,
            initialStackSymbol = 'Z'
        )
    }

    override fun canReachFinalState(input: StringBuilder, fromInit: Boolean): Boolean {
        return canReachFinalStatePDA(input, fromInit = fromInit, initialStack = if(fromInit) listOf('Z') else symbolStack)
    }

    private fun canReachFinalStatePDA(
        input: StringBuilder,
        fromInit:Boolean,
        initialStack: List<Char>
    ): Boolean {

        data class Path(
            val currentState: State,
            val inputIndex: Int,
            val symbolStack: List<Char>
        )

        var startState = states.firstOrNull { if(!fromInit) it.isCurrent else it.initial }
        if (startState == null) {
            setInitialStateAsCurrent()
            startState = states.firstOrNull { it.isCurrent }
        }
        if (startState == null) return false

        var paths = mutableListOf(Path(startState, 0, initialStack))

        while (paths.isNotEmpty()) {
            val nextPaths = mutableListOf<Path>()

            for (path in paths) {
                if (path.inputIndex == input.length && path.currentState.finite) {
                    return true
                }

                val currentChar = input.getOrNull(path.inputIndex)

                val possibleTransitions = transitions.filter {
                    it.startState == path.currentState.index &&
                            (
                                    it.name.isEmpty() || (currentChar != null && it.name.firstOrNull() == currentChar)
                                    )
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    val newStack = path.symbolStack.toMutableList()

                    if (transition is PushDownTransition) {
                        if (transition.pop.isNotEmpty()) {
                            val expectedTop = transition.pop.first()
                            if (newStack.isEmpty() || newStack.last() != expectedTop) continue
                            newStack.removeAt(newStack.lastIndex)
                        }

                        if (transition.push.isNotEmpty()) {
                            transition.push.reversed().forEach { symbol ->
                                newStack.add(symbol)
                            }
                        }
                    }

                    val newIndex = if (transition.name.isEmpty()) path.inputIndex else path.inputIndex + 1

                    nextPaths.add(
                        Path(
                            currentState = nextState,
                            inputIndex = newIndex,
                            symbolStack = newStack
                        )
                    )
                }
            }

            paths = nextPaths
        }

        return false
    }

    fun canReachInitialStackPDA(input: StringBuilder, fromInit: Boolean = false, symbolStack: List<Char>): Boolean {
        data class Path(
            val currentState: State,
            val inputIndex: Int,
            val symbolStack: List<Char>
        )

        val startState = states.firstOrNull { if(!fromInit) it.isCurrent else it.initial } ?: run {
            setInitialStateAsCurrent()
            states.firstOrNull { it.isCurrent }
        } ?: return false

        var paths = mutableListOf(Path(startState, 0, symbolStack))

        while (paths.isNotEmpty()) {
            val nextPaths = mutableListOf<Path>()

            for (path in paths) {
                if (path.inputIndex == input.length && path.symbolStack == listOf('Z')) {
                    return true
                }

                val currentChar = input.getOrNull(path.inputIndex)

                val possibleTransitions = transitions.filter {
                    it.startState == path.currentState.index &&
                            (it.name.isEmpty() || it.name.firstOrNull() == currentChar)
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    val newStack = path.symbolStack.toMutableList()

                    if (transition is PushDownTransition) {
                        if (transition.pop.isNotEmpty()) {
                            val expectedTop = transition.pop.first()
                            if (newStack.isEmpty() || newStack.last() != expectedTop) continue
                            newStack.removeAt(newStack.lastIndex)
                        }
                        if (transition.push.isNotEmpty()) {
                            transition.push.reversed().forEach { symbol ->
                                newStack.add(symbol)
                            }
                        }
                    }

                    val newIndex = if (transition.name.isEmpty()) path.inputIndex else path.inputIndex + 1

                    nextPaths.add(Path(nextState, newIndex, newStack))
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

            if (transition is PushDownTransition) {
                builder.appendLine("            <pop>${escapeXml(transition.pop)}</pop>")
                builder.appendLine("            <push>${escapeXml(transition.push)}</push>")
            } else {
                builder.appendLine("            <pop/>")
                builder.appendLine("            <push/>")
            }

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
        return transitions.filter { transition ->
            transition.startState == startState.index &&
                    input.startsWith(transition.name) &&
                    (transition !is PushDownTransition || transition.pop.isEmpty() ||
                            (symbolStack.isNotEmpty() && symbolStack.last() == transition.pop.first()))
        }
    }
}
