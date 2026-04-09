package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef

enum class AcceptanceCriteria(val text: String) {
    BY_FINAL_STATE("final state"),
    BY_EMPTY_STACK("empty stack"),
}

data class PdaConfig(val stateIndex: Int, val stack: List<Char>, val inputOffset: Int = 0)

class PushdownMachine(
    name: String = "",
    states: MutableList<State> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    val pdaTransitions: MutableList<PushdownTransition> = mutableListOf(),
    val symbolStack: MutableList<Char> = mutableListOf('Z'),
) : Machine(name, states, savedInputs = savedInputs) {
    override val jffTag = "pda"
    override val typeLabel = "PDA"

    override val transitions: List<Transition>
        get() = pdaTransitions

    // Track multiple current configs for NPDA simulation
    val currentConfigs: MutableList<PdaConfig> = mutableListOf()

    override var currentState: Int?
        get() = currentConfigs.firstOrNull()?.stateIndex
        set(value) {
            currentConfigs.clear()
            if (value != null) {
                currentConfigs.add(PdaConfig(value, listOf('Z')))
            }
            syncStack()
        }

    var acceptanceCriteria = AcceptanceCriteria.BY_FINAL_STATE
    val activeNodeStacks = mutableMapOf<Int, List<Char>>()
    var selectedNodeId: Int? = null

    override fun advanceSimulation(): Simulation {
        if (currentConfigs.isEmpty()) {
            if (!ensureCurrentState()) {
                return Simulation.Ended(SimulationOutcome.ACTIVE)
            }
            getStateByIndexOrNull(currentState!!)?.isCurrent = true
        }
        return calculateAllPathsStep()
    }

    override fun removeTransition(transition: Transition) {
        pdaTransitions.remove(transition)
    }

    override fun resetSimulation() {
        super.resetSimulation()
        currentConfigs.clear()
        activeNodeStacks.clear()
        selectedNodeId = null
        states
            .firstOrNull { it.initial }
            ?.index
            ?.let { currentConfigs.add(PdaConfig(it, listOf('Z'))) }
        syncStack()
    }

    fun selectNode(nodeId: Int) {
        if (nodeId !in activeNodeStacks) {
            return
        }
        selectedNodeId = nodeId
        activeNodeStacks[nodeId]?.let { stack ->
            symbolStack.clear()
            symbolStack.addAll(stack)
        }
    }

    fun addNewTransition(
        read: String,
        fromState: State,
        toState: State,
        pop: String,
        push: String,
    ) {
        val alreadyExists =
            pdaTransitions.any {
                it.read == read &&
                    it.fromState == fromState.index &&
                    it.toState == toState.index &&
                    it.pop == pop &&
                    it.push == push
            }
        if (!alreadyExists) {
            pdaTransitions.add(
                PushdownTransition(
                    fromState.index,
                    toState.index,
                    read = read,
                    pop = pop,
                    push = push,
                )
            )
        }
    }

    private fun calculateAllPathsStep(): Simulation {
        val allResults = mutableListOf<Triple<PdaConfig, PushdownTransition, PdaConfig>>()

        for (config in currentConfigs) {
            val state = getStateByIndexOrNull(config.stateIndex) ?: continue
            for (transition in getMatchingTransitions(state, config.stack, config.inputOffset)) {
                val newStack = config.stack.toMutableList()
                if (!applyStackOperation(transition, newStack)) {
                    continue
                }
                val newConfig =
                    PdaConfig(
                        transition.toState,
                        newStack,
                        config.inputOffset + transition.read.length,
                    )
                allResults.add(Triple(config, transition, newConfig))
            }
        }

        // No transitions available - check acceptance
        if (allResults.isEmpty()) {
            val acceptingConfigs =
                currentConfigs.filter { config ->
                    val state = getStateByIndexOrNull(config.stateIndex) ?: return@filter false
                    config.inputOffset >= remainingInput.length &&
                        when (acceptanceCriteria) {
                            AcceptanceCriteria.BY_FINAL_STATE -> state.final
                            AcceptanceCriteria.BY_EMPTY_STACK -> config.stack.isEmpty()
                        }
                }
            val anyAccepting = acceptingConfigs.isNotEmpty()
            return Simulation.Ended(
                outcome =
                    if (anyAccepting) SimulationOutcome.ACCEPTED else SimulationOutcome.REJECTED,
                isNodeAccepting =
                    if (anyAccepting)
                        { node ->
                            when (acceptanceCriteria) {
                                AcceptanceCriteria.BY_FINAL_STATE -> {
                                    val state = states.firstOrNull { it.name == node.stateName }
                                    state != null &&
                                        acceptingConfigs.any { it.stateIndex == state.index }
                                }

                                AcceptanceCriteria.BY_EMPTY_STACK ->
                                    activeNodeStacks[node.id]?.isEmpty() == true
                            }
                        }
                    else null,
            )
        }

        // Fire all transitions (epsilon + input) simultaneously
        val transitionRefs =
            allResults.map {
                TransitionRef(
                    it.first.stateIndex,
                    it.third.stateIndex,
                    pdaTransitions.indexOf(it.second),
                )
            }
        val newConfigs = allResults.map { it.third }.toSet()

        // Detect no-progress loop (e.g. epsilon cycle with unchanged stacks)
        if (newConfigs == currentConfigs.toSet()) {
            val acceptingConfigs =
                currentConfigs.filter { config ->
                    val state = getStateByIndexOrNull(config.stateIndex) ?: return@filter false
                    config.inputOffset >= remainingInput.length &&
                        when (acceptanceCriteria) {
                            AcceptanceCriteria.BY_FINAL_STATE -> state.final
                            AcceptanceCriteria.BY_EMPTY_STACK -> config.stack.isEmpty()
                        }
                }
            val anyAccepting = acceptingConfigs.isNotEmpty()
            return Simulation.Ended(
                outcome =
                    if (anyAccepting) SimulationOutcome.ACCEPTED else SimulationOutcome.REJECTED,
                isNodeAccepting =
                    if (anyAccepting)
                        { node ->
                            when (acceptanceCriteria) {
                                AcceptanceCriteria.BY_FINAL_STATE -> {
                                    val state = states.firstOrNull { it.name == node.stateName }
                                    state != null &&
                                        acceptingConfigs.any { it.stateIndex == state.index }
                                }

                                AcceptanceCriteria.BY_EMPTY_STACK ->
                                    activeNodeStacks[node.id]?.isEmpty() == true
                            }
                        }
                    else null,
            )
        }

        // Consume input now so the tape head advances at animation start
        val minOffset = newConfigs.minOf { it.inputOffset }
        consumeInput(minOffset)
        val adjustedConfigs =
            newConfigs.map { it.copy(inputOffset = it.inputOffset - minOffset) }.toSet()

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

    private fun syncStack() {
        symbolStack.clear()
        val config = currentConfigs.firstOrNull()
        if (config != null) {
            symbolStack.addAll(config.stack)
        } else {
            symbolStack.add('Z')
        }
        buildActiveNodeStacks()
    }

    private fun buildActiveNodeStacks() {
        activeNodeStacks.clear()
        val active = tree.getActiveNodes()
        if (active.isEmpty() || currentConfigs.isEmpty()) {
            return
        }

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
            if (node.id in activeNodeStacks) {
                continue
            }
            val state = states.firstOrNull { it.name == node.stateName } ?: continue
            for (config in currentConfigs) {
                val transition =
                    pdaTransitions.firstOrNull { transition ->
                        transition.fromState == config.stateIndex &&
                            transition.toState == state.index
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

    private fun applyStackOperation(
        transition: PushdownTransition,
        stack: MutableList<Char>,
    ): Boolean {
        if (transition.pop.isNotEmpty()) {
            if (stack.size < transition.pop.length) {
                return false
            }
            if (!stackTopMatches(stack, transition.pop)) {
                return false
            }
            repeat(transition.pop.length) { stack.removeAt(stack.lastIndex) }
        }
        if (transition.push.isNotEmpty()) {
            transition.push.reversed().forEach { stack.add(it) }
        }
        return true
    }

    private fun getMatchingTransitions(
        fromState: State,
        stack: List<Char>,
        inputOffset: Int,
    ): List<PushdownTransition> {
        val remaining =
            if (inputOffset < remainingInput.length) {
                remainingInput.substring(inputOffset)
            } else {
                ""
            }
        return pdaTransitions.filter { transition ->
            transition.fromState == fromState.index &&
                (transition.read.isEmpty() || remaining.startsWith(transition.read)) &&
                (transition.pop.isEmpty() || stackTopMatches(stack, transition.pop))
        }
    }

    private fun stackTopMatches(stack: List<Char>, pop: String): Boolean {
        if (stack.size < pop.length) {
            return false
        }
        for (i in pop.indices) {
            if (stack[stack.size - 1 - i] != pop[i]) {
                return false
            }
        }
        return true
    }
}
