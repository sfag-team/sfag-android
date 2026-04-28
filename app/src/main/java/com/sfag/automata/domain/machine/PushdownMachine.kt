package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.main.config.MAX_SIM_PDA_CONFIGS
import com.sfag.main.config.MAX_SIM_PDA_STALE_STEPS
import com.sfag.main.config.Symbols

enum class AcceptanceCriteria {
    BY_FINAL_STATE,
    BY_EMPTY_STACK,
}

class PushdownMachine(
    name: String = "",
    states: MutableList<State> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    val pdaTransitions: MutableList<PdaTransition> = mutableListOf(),
) : Machine(name, states, savedInputs = savedInputs) {
    override val jffTag = "pda"
    override val typeLabel = "PDA"

    override val transitions: List<Transition>
        get() = pdaTransitions

    // Track multiple current configs for NPDA simulation
    val currentConfigs: MutableList<Config.Pda> = mutableListOf()

    var acceptanceCriteria = AcceptanceCriteria.BY_FINAL_STATE
    private var staleStepCount = 0

    override fun removeTransition(transition: Transition) {
        pdaTransitions.remove(transition)
    }

    override fun resetSimulation() {
        currentConfigs.clear()
        staleStepCount = 0
        initialState?.index?.let {
            currentConfigs.add(
                Config.Pda(it, listOf(Symbols.INITIAL_STACK_SYMBOL), treeNodeId = tree.root!!.id)
            )
        }
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

    override fun advanceSimulation(): Simulation {
        // Config limit - prevent runaway branching
        if (currentConfigs.size > MAX_SIM_PDA_CONFIGS) {
            return terminateSimulation()
        }

        val stepResults = mutableListOf<StepResult<Config.Pda>>()
        for (config in currentConfigs) {
            val state = getStateByIndex(config.stateIndex)
            for (transition in getMatchingTransitions(state, config.stack, config.inputConsumed)) {
                val newStack =
                    applyStackOp(config.stack, transition.pop, transition.push) ?: continue
                val newConfig =
                    Config.Pda(
                        transition.toState,
                        newStack,
                        config.inputConsumed + transition.read.length,
                    )
                stepResults.add(StepResult(src = config, dest = newConfig, transition = transition))
            }
        }
        if (stepResults.isEmpty()) {
            return terminateSimulation()
        }

        // No-progress loop: same (state, stack, consumed) set, ignoring treeNodeId.
        val newKeys =
            stepResults.mapTo(mutableSetOf()) {
                Triple(it.dest.stateIndex, it.dest.stack, it.dest.inputConsumed)
            }
        val currentKeys =
            currentConfigs.mapTo(mutableSetOf()) {
                Triple(it.stateIndex, it.stack, it.inputConsumed)
            }
        if (newKeys == currentKeys) {
            return terminateSimulation()
        }

        // Stale step detection - the slowest path didn't advance the input floor.
        val newFloor = stepResults.minOf { it.dest.inputConsumed }
        val curFloor = currentConfigs.minOf { it.inputConsumed }
        if (newFloor == curFloor) {
            staleStepCount++
            if (staleStepCount > MAX_SIM_PDA_STALE_STEPS) {
                return terminateSimulation()
            }
        } else {
            staleStepCount = 0
        }

        val (treeBranches, pendingConfigs) = buildBranches(currentConfigs, stepResults)

        return Simulation.Step(
            transitionRefs = buildTransitionRefs(stepResults),
            treeBranches = treeBranches,
            onAllComplete = {
                currentConfigs.clear()
                for ((branch, config) in pendingConfigs) {
                    currentConfigs.add(config.copy(treeNodeId = branch.treeNodeId))
                }
            },
        )
    }

    private fun terminateSimulation(): Simulation.Ended =
        terminate(
            currentConfigs.filter {
                it.inputConsumed >= fullInput.length &&
                    when (acceptanceCriteria) {
                        AcceptanceCriteria.BY_FINAL_STATE -> getStateByIndex(it.stateIndex).final
                        AcceptanceCriteria.BY_EMPTY_STACK -> it.stack.isEmpty()
                    }
            }
        )

    private fun getMatchingTransitions(
        fromState: State,
        stack: List<Char>,
        inputConsumed: Int,
    ): List<PdaTransition> {
        val remaining =
            if (inputConsumed < fullInput.length) fullInput.substring(inputConsumed) else ""
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
