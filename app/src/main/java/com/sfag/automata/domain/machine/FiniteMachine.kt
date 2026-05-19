package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.SimAdvance
import com.sfag.automata.domain.simulation.SimConfig
import com.sfag.automata.domain.simulation.StepResult

class FiniteMachine(
    name: String = "",
    states: MutableList<State> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    val faTransitions: MutableList<FaTransition> = mutableListOf(),
) : Machine(name, states, savedInputs = savedInputs) {
    override val jffTag = "fa"

    override val transitions: List<Transition>
        get() = faTransitions

    // Track multiple current configs for NFA simulation
    val activeConfigs: MutableList<SimConfig.Fa> = mutableListOf()

    override fun removeTransition(transition: Transition) {
        faTransitions.remove(transition)
    }

    override fun resetSimulation() {
        activeConfigs.clear()
        initialState?.index?.let {
            activeConfigs.add(SimConfig.Fa(it, treeNodeId = tree.root!!.id))
        }
    }

    fun addNewTransition(fromState: State, toState: State, read: String) {
        val transition = FaTransition(fromState.index, toState.index, read)
        if (transition !in faTransitions) {
            faTransitions.add(transition)
        }
    }

    override fun advanceSimulation(): SimAdvance {
        val stepResults = mutableListOf<StepResult<SimConfig.Fa>>()
        for (config in activeConfigs) {
            val state = getStateByIndex(config.stateIndex)
            for (transition in getMatchingTransitions(state, config.inputConsumed)) {
                val newConfig =
                    SimConfig.Fa(transition.toState, config.inputConsumed + transition.read.length)
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

        return buildStep(stepResults, activeConfigs)
    }

    private fun terminateSimulation(): SimAdvance.Ended =
        terminate(
            activeConfigs.filter {
                getStateByIndex(it.stateIndex).final && it.inputConsumed >= fullInput.length
            }
        )

    private fun getMatchingTransitions(fromState: State, inputConsumed: Int): List<FaTransition> {
        val remaining =
            if (inputConsumed < fullInput.length) fullInput.substring(inputConsumed) else ""
        return faTransitions.filter {
            it.fromState == fromState.index && (it.read.isEmpty() || remaining.startsWith(it.read))
        }
    }
}
