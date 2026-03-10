package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef

private const val MAX_REACHABILITY_CONFIGS = 100_000

enum class AcceptanceCriteria(
    val text: String,
) {
    BY_FINAL_STATE("final state"),
    BY_EMPTY_STACK("empty stack"),
}

data class PdaConfig(
    val stateIndex: Int,
    val stack: List<Char>,
    val inputOffset: Int = 0,
)

class PushdownMachine(
    name: String = "",
    states: MutableList<State> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    val pdaTransitions: MutableList<PushdownTransition> = mutableListOf(),
    val symbolStack: MutableList<Char> = mutableListOf('Z'),
) : Machine(name, states, savedInputs = savedInputs) {
    override val jffTag = "pda"
    override val typeLabel = "PDA"
    var acceptanceCriteria = AcceptanceCriteria.BY_FINAL_STATE

    val currentConfigs: MutableList<PdaConfig> = mutableListOf()

    val activeNodeStacks = mutableMapOf<Int, List<Char>>()
    var selectedNodeId: Int? = null

    override var currentState: Int?
        get() = currentConfigs.firstOrNull()?.stateIndex
        set(value) {
            currentConfigs.clear()
            if (value != null) {
                currentConfigs.add(PdaConfig(value, listOf('Z')))
            }
            syncStack()
        }

    override val transitions: List<Transition>
        get() = pdaTransitions

    override fun removeTransition(transition: Transition) {
        pdaTransitions.remove(transition)
    }

    override fun advanceSimulation(): Simulation {
        if (currentConfigs.isEmpty()) {
            if (!ensureCurrentState()) return Simulation.Ended(SimulationOutcome.ACTIVE)
            getStateByIndexOrNull(currentState!!)?.isCurrent = true
        }
        return calculateAllPathsStep()
    }

    private fun calculateAllPathsStep(): Simulation {
        val epsilonResults = mutableListOf<Triple<PdaConfig, PushdownTransition, PdaConfig>>()
        val inputResults = mutableListOf<Triple<PdaConfig, PushdownTransition, PdaConfig>>()

        for (config in currentConfigs) {
            val state = getStateByIndexOrNull(config.stateIndex) ?: continue
            for (transition in getMatchingTransitions(state, config.stack, config.inputOffset)) {
                val newStack = config.stack.toMutableList()
                if (!applyStackOperation(transition, newStack)) continue
                if (transition.name.isEmpty()) {
                    val newConfig = PdaConfig(transition.toState, newStack, config.inputOffset)
                    epsilonResults.add(Triple(config, transition, newConfig))
                } else {
                    val newConfig = PdaConfig(
                        transition.toState,
                        newStack,
                        config.inputOffset + transition.name.length,
                    )
                    inputResults.add(Triple(config, transition, newConfig))
                }
            }
        }

        // Process epsilon transitions first - add configs that don't already exist
        if (epsilonResults.isNotEmpty()) {
            val newConfigs = epsilonResults.map { it.third }.filter { it !in currentConfigs }
            if (newConfigs.isNotEmpty()) {
                val newConfigSet = newConfigs.toSet()
                val transitionRefs =
                    epsilonResults
                        .filter { it.third in newConfigSet }
                        .map { TransitionRef(it.first.stateIndex, it.third.stateIndex) }
                        .distinct()

                expandSimulationTree(transitionRefs, keepActive = true)

                return Simulation.Step(
                    transitionRefs = transitionRefs,
                    onAllComplete = {
                        for (config in newConfigs) {
                            currentConfigs.add(config)
                            getStateByIndexOrNull(config.stateIndex)?.isCurrent = true
                        }
                        syncStack()
                    },
                )
            }
        }

        // No input transitions - check acceptance
        if (inputResults.isEmpty()) {
            val anyAccepting =
                currentConfigs.any { config ->
                    val state = getStateByIndexOrNull(config.stateIndex) ?: return@any false
                    config.inputOffset >= currentInput.length &&
                        when (acceptanceCriteria) {
                            AcceptanceCriteria.BY_FINAL_STATE -> state.final
                            AcceptanceCriteria.BY_EMPTY_STACK -> config.stack.isEmpty()
                        }
                }
            if (anyAccepting) {
                val acceptedIds =
                    tree
                        .getActiveNodes()
                        .filter { node ->
                            when (acceptanceCriteria) {
                                AcceptanceCriteria.BY_FINAL_STATE ->
                                    states.any { it.name == node.stateName && it.final }
                                AcceptanceCriteria.BY_EMPTY_STACK ->
                                    activeNodeStacks[node.id]?.isEmpty() == true
                            }
                        }
                        .map { it.id }
                        .toSet()
                tree.markAcceptedPaths(acceptedIds)
            }
            tree.markRemainingAsRejected()
            return Simulation.Ended(
                if (anyAccepting) SimulationOutcome.ACCEPTED else SimulationOutcome.REJECTED,
            )
        }

        // Process ALL input transitions - fire all lengths simultaneously
        val transitionRefs =
            inputResults
                .map { TransitionRef(it.first.stateIndex, it.third.stateIndex) }
                .distinct()
        val newConfigs = inputResults.map { it.third }

        expandSimulationTree(transitionRefs)

        // Advance shared input by minimum new offset
        val minOffset = newConfigs.minOf { it.inputOffset }
        consumeInput(minOffset)

        // Adjust offsets relative to new input position
        val adjustedConfigs = newConfigs.map { it.copy(inputOffset = it.inputOffset - minOffset) }

        return Simulation.Step(
            transitionRefs = transitionRefs,
            onAllComplete = {
                for (config in currentConfigs) {
                    getStateByIndexOrNull(config.stateIndex)?.isCurrent = false
                }
                currentConfigs.clear()
                currentConfigs.addAll(adjustedConfigs)
                for (config in currentConfigs) {
                    getStateByIndexOrNull(config.stateIndex)?.isCurrent = true
                }
                syncStack()
            },
        )
    }

    private fun consumeInput(length: Int) {
        if (length > 0) {
            currentInput.delete(0, length)
            if (remainingInput.isNotEmpty()) {
                remainingInput.delete(0, minOf(length, remainingInput.length))
            }
        }
    }

    private fun syncStack() {
        symbolStack.clear()
        val bestConfig =
            if (currentConfigs.size <= 1) {
                currentConfigs.firstOrNull()
            } else {
                // NPDA: prefer the config on the accepting path
                val acceptPredicate: (State, List<Char>) -> Boolean =
                    when (acceptanceCriteria) {
                        AcceptanceCriteria.BY_FINAL_STATE -> { state, _ -> state.final }
                        AcceptanceCriteria.BY_EMPTY_STACK -> { _, s -> s.isEmpty() }
                    }
                currentConfigs.firstOrNull { config ->
                    val remaining = currentInput.substring(
                        minOf(config.inputOffset, currentInput.length),
                    )
                    canReachAcceptingState(
                        StringBuilder(remaining),
                        config.stateIndex,
                        config.stack,
                        acceptPredicate,
                    ) == true
                } ?: currentConfigs.firstOrNull()
            }
        if (bestConfig != null) {
            symbolStack.addAll(bestConfig.stack)
        } else {
            symbolStack.add('Z')
        }
        buildActiveNodeStacks()
    }

    private fun buildActiveNodeStacks() {
        activeNodeStacks.clear()
        val active = tree.getActiveNodes()
        if (active.isEmpty() || currentConfigs.isEmpty()) return

        // Greedy-match each active node to a config by state index
        val remainingConfigs = currentConfigs.toMutableList()
        for (node in active) {
            val state = states.firstOrNull { it.name == node.stateName } ?: continue
            val matchIndex = remainingConfigs.indexOfFirst { it.stateIndex == state.index }
            if (matchIndex >= 0) {
                activeNodeStacks[node.id] = remainingConfigs[matchIndex].stack
                remainingConfigs.removeAt(matchIndex)
            }
        }

        // Infer stack for unmatched nodes (e.g. tree expanded for transitions
        // not yet processed - epsilon step shows input-transition branches too)
        for (node in active) {
            if (node.id in activeNodeStacks) continue
            val state = states.firstOrNull { it.name == node.stateName } ?: continue
            for (config in currentConfigs) {
                val transition =
                    pdaTransitions.firstOrNull { transition ->
                        transition.fromState == config.stateIndex && transition.toState == state.index
                    } ?: continue
                val inferredStack = config.stack.toMutableList()
                if (applyStackOperation(transition, inferredStack)) {
                    activeNodeStacks[node.id] = inferredStack
                    break
                }
            }
        }

        // Follow same branch as previously selected node, picking the first
        // child alphabetically by state name - keeps the displayed stack stable
        if (selectedNodeId != null && selectedNodeId !in activeNodeStacks) {
            val prevNode = tree.findNode(selectedNodeId!!)
            selectedNodeId =
                prevNode
                    ?.children
                    ?.filter { it.id in activeNodeStacks }
                    ?.minByOrNull { it.stateName ?: "" }
                    ?.id ?: activeNodeStacks.keys.minOrNull()
        } else if (selectedNodeId == null) {
            selectedNodeId = activeNodeStacks.keys.minOrNull()
        }

        // Update symbolStack from selected node's stack
        selectedNodeId?.let { id ->
            activeNodeStacks[id]?.let { stack ->
                symbolStack.clear()
                symbolStack.addAll(stack)
            }
        }
    }

    fun selectNode(nodeId: Int) {
        if (nodeId !in activeNodeStacks) return
        selectedNodeId = nodeId
        activeNodeStacks[nodeId]?.let { stack ->
            symbolStack.clear()
            symbolStack.addAll(stack)
        }
    }

    fun addNewTransition(
        name: String,
        fromState: State,
        toState: State,
        pop: String,
        stackTop: String, // peek symbol - consumed and re-pushed when no explicit pop is given
        push: String,
    ) {
        val resolvedPop: String
        val resolvedPush: String
        when {
            pop.isNotEmpty() -> {
                resolvedPop = pop
                resolvedPush = ""
            }
            stackTop.isNotEmpty() -> {
                resolvedPop = stackTop
                resolvedPush = push + stackTop
            }
            else -> {
                resolvedPop = ""
                resolvedPush = push
            }
        }
        val alreadyExists =
            pdaTransitions.any {
                it.name == name &&
                    it.fromState == fromState.index &&
                    it.toState == toState.index &&
                    it.pop == resolvedPop &&
                    it.push == resolvedPush
            }
        if (!alreadyExists) {
            pdaTransitions.add(
                PushdownTransition(
                    name,
                    fromState.index,
                    toState.index,
                    pop = resolvedPop,
                    push = resolvedPush,
                ),
            )
        }
    }

    override fun resetSimulation() {
        val initialStateIndex = states.firstOrNull { it.initial }?.index
        for (state in states) {
            if (state.index != initialStateIndex) state.isCurrent = false
        }
        currentConfigs.clear()
        activeNodeStacks.clear()
        selectedNodeId = null
        if (initialStateIndex != null) currentConfigs.add(PdaConfig(initialStateIndex, listOf('Z')))
        syncStack()
    }

    override fun isAccepted(input: StringBuilder): Boolean? =
        if (acceptanceCriteria == AcceptanceCriteria.BY_FINAL_STATE) {
            canReachFinalState(input, true)
        } else {
            canReachAcceptingState(input, findStartStateIndex(true), listOf('Z')) { _, s -> s.isEmpty() }
        }

    override fun canReachFinalState(
        input: StringBuilder,
        fromInit: Boolean,
    ): Boolean? {
        val startIndex = findStartStateIndex(fromInit) ?: return false
        val stack =
            if (fromInit) listOf('Z') else currentConfigs.firstOrNull()?.stack ?: listOf('Z')
        return canReachAcceptingState(input, startIndex, stack) { state, _ -> state.final }
    }

    private fun applyStackOperation(
        transition: PushdownTransition,
        stack: MutableList<Char>,
    ): Boolean {
        if (transition.pop.isNotEmpty()) {
            if (stack.size < transition.pop.length) return false
            if (!stackTopMatches(stack, transition.pop)) return false
            repeat(transition.pop.length) { stack.removeAt(stack.lastIndex) }
        }
        if (transition.push.isNotEmpty()) {
            transition.push.reversed().forEach { stack.add(it) }
        }
        return true
    }

    private fun canReachAcceptingState(
        input: StringBuilder,
        startStateIndex: Int?,
        stack: List<Char>,
        isAccepted: (State, List<Char>) -> Boolean,
    ): Boolean? {
        data class BfsPath(
            val stateIndex: Int,
            val inputIndex: Int,
            val stack: List<Char>,
        )

        val startIndex = startStateIndex ?: return false
        var paths = mutableListOf(BfsPath(startIndex, 0, stack))
        var configCount = 0
        val maxConfigs = MAX_REACHABILITY_CONFIGS

        while (paths.isNotEmpty() && configCount < maxConfigs) {
            val nextPaths = mutableListOf<BfsPath>()

            for (path in paths) {
                configCount++
                if (configCount > maxConfigs) break

                val state = getStateByIndexOrNull(path.stateIndex) ?: continue
                if (path.inputIndex == input.length && isAccepted(state, path.stack)) {
                    return true
                }

                val remaining = input.substring(path.inputIndex)
                val possibleTransitions =
                    pdaTransitions.filter {
                        it.fromState == path.stateIndex &&
                            (it.name.isEmpty() || remaining.startsWith(it.name))
                    }

                for (transition in possibleTransitions) {
                    val newStack = path.stack.toMutableList()
                    if (!applyStackOperation(transition, newStack)) continue
                    nextPaths.add(
                        BfsPath(
                            transition.toState,
                            path.inputIndex + transition.name.length,
                            newStack,
                        ),
                    )
                }
            }

            paths = nextPaths
        }

        return if (paths.isEmpty()) false else null
    }

    private fun getMatchingTransitions(
        fromState: State,
        stack: List<Char>,
        inputOffset: Int,
    ): List<PushdownTransition> {
        val remaining =
            if (inputOffset < currentInput.length) {
                currentInput.substring(inputOffset)
            } else {
                ""
            }
        return pdaTransitions.filter { transition ->
            transition.fromState == fromState.index &&
                (transition.name.isEmpty() || remaining.startsWith(transition.name)) &&
                (transition.pop.isEmpty() || stackTopMatches(stack, transition.pop))
        }
    }

    private fun stackTopMatches(
        stack: List<Char>,
        pop: String,
    ): Boolean {
        if (stack.size < pop.length) return false
        for (i in pop.indices) {
            if (stack[stack.size - 1 - i] != pop[i]) return false
        }
        return true
    }
}
