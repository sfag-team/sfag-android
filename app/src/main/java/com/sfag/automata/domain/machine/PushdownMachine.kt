package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef
import com.sfag.automata.domain.tree.Branch
import com.sfag.main.config.MAX_SIM_PDA_CONFIGS
import com.sfag.main.config.MAX_SIM_PDA_STALE_STEPS
import com.sfag.main.config.Symbols

enum class AcceptanceCriteria(val text: String) {
    BY_FINAL_STATE("final state"),
    BY_EMPTY_STACK("empty stack"),
}

data class PdaConfig(
    val stateIndex: Int,
    val stack: List<Char>,
    val inputOffset: Int = 0,
    val treeNodeId: Int = -1,
)

class PushdownMachine(
    name: String = "",
    states: MutableList<State> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    val pdaTransitions: MutableList<PdaTransition> = mutableListOf(),
    val symbolStack: MutableList<Char> = mutableListOf(Symbols.INITIAL_STACK_SYMBOL),
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
                currentConfigs.add(PdaConfig(value, listOf(Symbols.INITIAL_STACK_SYMBOL)))
            }
            syncStack()
        }

    var acceptanceCriteria = AcceptanceCriteria.BY_FINAL_STATE
    var selectedNodeId: Int? = null
    private var staleStepCount = 0

    private fun activeNodeIds(): List<Int> =
        currentConfigs.mapNotNull { it.treeNodeId.takeIf { id -> id >= 0 } }

    private fun stackForNode(nodeId: Int): List<Char>? =
        currentConfigs.firstOrNull { it.treeNodeId == nodeId }?.stack

    override fun removeTransition(transition: Transition) {
        pdaTransitions.remove(transition)
    }

    override fun resetSimulation() {
        super.resetSimulation()
        currentConfigs.clear()
        selectedNodeId = null
        staleStepCount = 0
        initialState?.index?.let {
            currentConfigs.add(
                PdaConfig(it, listOf(Symbols.INITIAL_STACK_SYMBOL), treeNodeId = tree.root!!.id)
            )
        }
        syncStack()
    }

    fun selectNode(nodeId: Int) {
        val stack = stackForNode(nodeId) ?: return
        selectedNodeId = nodeId
        symbolStack.clear()
        symbolStack.addAll(stack)
    }

    fun addNewTransition(
        fromState: State,
        toState: State,
        read: String,
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
                PdaTransition(fromState.index, toState.index, read = read, pop = pop, push = push)
            )
        }
    }

    override fun calculateAllPathsStep(): Simulation {
        // Config limit - prevent runaway branching
        if (currentConfigs.size > MAX_SIM_PDA_CONFIGS) {
            return terminateSimulation()
        }

        val allResults = mutableListOf<Triple<PdaConfig, PdaTransition, PdaConfig>>()

        for (config in currentConfigs) {
            val state = getStateByIndex(config.stateIndex)
            for (transition in getMatchingTransitions(state, config.stack, config.inputOffset)) {
                val newStack =
                    applyStackOp(config.stack, transition.pop, transition.push) ?: continue
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
            return terminateSimulation()
        }

        // Build treeBranches and pair each new config with its branch (the branch receives
        // its tree node id when the tree expands, so configs can read it back directly)
        val resultsByParent = allResults.groupBy { it.first.treeNodeId }
        val treeBranches = linkedMapOf<Int, MutableList<Branch>>()
        val pendingConfigs = mutableListOf<Pair<Branch, PdaConfig>>()

        for (config in currentConfigs) {
            val results = resultsByParent[config.treeNodeId] ?: continue
            val branches = mutableListOf<Branch>()
            for ((_, transition, newConfig) in results) {
                val toState = getStateByIndex(transition.toState)
                val branch = Branch(toState.name)
                branches.add(branch)
                pendingConfigs.add(branch to newConfig)
            }
            treeBranches[config.treeNodeId] = branches
        }

        // Build transitionRefs for animation
        val transitionRefs =
            allResults.map {
                TransitionRef(
                    it.first.stateIndex,
                    it.third.stateIndex,
                    pdaTransitions.indexOf(it.second),
                )
            }

        // No-progress loop: same (state, stack, offset) set — treeNodeId excluded from the key
        val newKeys = allResults.mapTo(mutableSetOf()) { it.third.copy(treeNodeId = -1) }
        val currentKeys = currentConfigs.mapTo(mutableSetOf()) { it.copy(treeNodeId = -1) }
        if (newKeys == currentKeys) {
            return terminateSimulation()
        }

        val minOffset = allResults.minOf { it.third.inputOffset }

        // Stale step detection - tape not advancing
        if (minOffset == 0) {
            staleStepCount++
            if (staleStepCount > MAX_SIM_PDA_STALE_STEPS) {
                return terminateSimulation()
            }
        } else {
            staleStepCount = 0
        }

        return Simulation.Step(
            transitionRefs = transitionRefs,
            treeBranches = treeBranches,
            onAllComplete = {
                consumeInput(minOffset)
                for (config in currentConfigs) {
                    getStateByIndex(config.stateIndex).isCurrent = false
                }
                currentConfigs.clear()

                for ((branch, config) in pendingConfigs) {
                    currentConfigs.add(
                        config.copy(
                            inputOffset = config.inputOffset - minOffset,
                            treeNodeId = branch.assignedId,
                        )
                    )
                }

                for (config in currentConfigs) {
                    getStateByIndex(config.stateIndex).isCurrent = true
                }
                syncStack()
            },
        )
    }

    private fun terminateSimulation(): Simulation.Ended {
        val acceptingConfigs =
            currentConfigs.filter { config ->
                val state = getStateByIndex(config.stateIndex)
                config.inputOffset >= remainingInput.length &&
                    when (acceptanceCriteria) {
                        AcceptanceCriteria.BY_FINAL_STATE -> state.final
                        AcceptanceCriteria.BY_EMPTY_STACK -> config.stack.isEmpty()
                    }
            }
        val anyAccepting = acceptingConfigs.isNotEmpty()
        return Simulation.Ended(
            outcome = if (anyAccepting) SimulationOutcome.ACCEPTED else SimulationOutcome.REJECTED,
            isNodeAccepting =
                if (anyAccepting) { node -> acceptingConfigs.any { it.treeNodeId == node.id } }
                else {
                    null
                },
        )
    }

    private fun syncStack() {
        symbolStack.clear()
        val config = currentConfigs.firstOrNull()
        if (config != null) {
            symbolStack.addAll(config.stack)
        } else {
            symbolStack.add(Symbols.INITIAL_STACK_SYMBOL)
        }
        buildActiveNodeStacks()
    }

    private fun buildActiveNodeStacks() {
        if (currentConfigs.isEmpty()) {
            return
        }

        val activeIds = activeNodeIds()

        // Follow same branch as previously selected node, picking the first
        // child alphabetically by state name - keeps the displayed stack stable
        if (selectedNodeId != null && selectedNodeId !in activeIds) {
            val prevNode = tree.findNode(selectedNodeId!!)
            selectedNodeId =
                prevNode?.children?.filter { it.id in activeIds }?.minByOrNull { it.stateName }?.id
                    ?: activeIds.minOrNull()
        } else if (selectedNodeId == null) {
            selectedNodeId = activeIds.minOrNull()
        }

        selectedNodeId?.let { id ->
            stackForNode(id)?.let { stack ->
                symbolStack.clear()
                symbolStack.addAll(stack)
            }
        }
    }

    private fun getMatchingTransitions(
        fromState: State,
        stack: List<Char>,
        inputOffset: Int,
    ): List<PdaTransition> {
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
}

/** True if the top of [stack] (rightmost first) matches [pop]. */
internal fun stackTopMatches(stack: List<Char>, pop: String): Boolean {
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

/**
 * Applies a PDA pop/push to [stack], returning a new list. Returns null if the pop precondition is
 * not satisfied. Pure - does not mutate the input stack.
 */
internal fun applyStackOp(stack: List<Char>, pop: String, push: String): List<Char>? {
    if (pop.isNotEmpty() && !stackTopMatches(stack, pop)) {
        return null
    }
    val result = stack.toMutableList()
    repeat(pop.length) { result.removeAt(result.lastIndex) }
    push.reversed().forEach { result.add(it) }
    return result
}
