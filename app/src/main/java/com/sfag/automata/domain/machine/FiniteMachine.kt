package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation

class FiniteMachine(
    name: String = "",
    states: MutableList<State> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    override val transitions: MutableList<Transition> = mutableListOf(),
) : Machine(name, states, savedInputs = savedInputs) {
    override val jffTag = "fa"
    override val typeLabel = "FA"

    // Track multiple current configs for NFA simulation
    val activeConfigs: MutableList<Config.Fa> = mutableListOf()

    override fun removeTransition(transition: Transition) {
        transitions.remove(transition)
    }

    override fun resetSimulation() {
        activeConfigs.clear()
        initialState?.index?.let { activeConfigs.add(Config.Fa(it, treeNodeId = tree.root!!.id)) }
    }

    fun addNewTransition(fromState: State, toState: State, read: String) {
        val alreadyExists =
            transitions.any { transition ->
                transition.fromState == fromState.index &&
                    transition.toState == toState.index &&
                    transition.read == read
            }
        if (!alreadyExists) {
            transitions.add(FaTransition(fromState.index, toState.index, read))
        }
    }

    override fun advanceSimulation(): Simulation {
        val stepResults = mutableListOf<StepResult<Config.Fa>>()
        for (config in activeConfigs) {
            val state = getStateByIndex(config.stateIndex)
            for (transition in getMatchingTransitions(state, config.inputConsumed)) {
                val newConfig =
                    Config.Fa(transition.toState, config.inputConsumed + transition.read.length)
                stepResults.add(StepResult(src = config, dest = newConfig, transition = transition))
            }
        }
        if (stepResults.isEmpty()) {
            return terminateSimulation()
        }

        // No-progress loop: same (state, consumed) set, ignoring treeNodeId
        val newKeys = stepResults.map { it.dest.stateIndex to it.dest.inputConsumed }.toSet()
        val currentKeys = activeConfigs.map { it.stateIndex to it.inputConsumed }.toSet()
        if (newKeys == currentKeys) {
            return terminateSimulation()
        }

        val (treeBranches, pendingConfigs) = buildBranches(activeConfigs, stepResults)

        return Simulation.Step(
            transitionRefs = buildTransitionRefs(stepResults),
            treeBranches = treeBranches,
            onAllComplete = {
                activeConfigs.clear()
                for ((branch, config) in pendingConfigs) {
                    activeConfigs.add(config.copy(treeNodeId = branch.treeNodeId))
                }
            },
        )
    }

    private fun terminateSimulation(): Simulation.Ended =
        terminate(
            activeConfigs.filter {
                getStateByIndex(it.stateIndex).final && it.inputConsumed >= fullInput.length
            }
        )

    private fun getMatchingTransitions(fromState: State, inputConsumed: Int): List<Transition> {
        val remaining =
            if (inputConsumed < fullInput.length) fullInput.substring(inputConsumed) else ""
        return transitions.filter {
            it.fromState == fromState.index && (it.read.isEmpty() || remaining.startsWith(it.read))
        }
    }
}
