package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef
import com.sfag.automata.domain.tree.Branch

data class FaConfig(val stateIndex: Int, val inputOffset: Int = 0, val treeNodeId: Int = -1)

class FiniteMachine(
    name: String = "",
    states: MutableList<State> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    override val transitions: MutableList<Transition> = mutableListOf(),
) : Machine(name, states, savedInputs = savedInputs) {
    override val jffTag = "fa"
    override val typeLabel = "FA"

    // Track multiple current configs for NFA simulation
    val currentConfigs: MutableList<FaConfig> = mutableListOf()

    override var currentState: Int?
        get() = currentConfigs.firstOrNull()?.stateIndex
        set(value) {
            currentConfigs.clear()
            value?.let { currentConfigs.add(FaConfig(it)) }
        }

    override fun removeTransition(transition: Transition) {
        transitions.remove(transition)
    }

    override fun resetSimulation() {
        super.resetSimulation()
        currentConfigs.clear()
        initialState?.index?.let { currentConfigs.add(FaConfig(it, treeNodeId = tree.root!!.id)) }
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

    override fun calculateAllPathsStep(): Simulation {
        val allResults = mutableListOf<Triple<FaConfig, Transition, FaConfig>>()

        for (config in currentConfigs) {
            val state = getStateByIndex(config.stateIndex)
            for (transition in getMatchingTransitions(state, config.inputOffset)) {
                val newConfig =
                    FaConfig(transition.toState, config.inputOffset + transition.read.length)
                allResults.add(Triple(config, transition, newConfig))
            }
        }

        if (allResults.isEmpty()) {
            return terminateSimulation()
        }

        // Pair each new config with a Branch; the tree fills in branch.assignedId on expand,
        // and the config reads it back at onAllComplete time (no positional coupling)
        val resultsByParent = allResults.groupBy { it.first.treeNodeId }
        val treeBranches = linkedMapOf<Int, MutableList<Branch>>()
        val pendingConfigs = mutableListOf<Pair<Branch, FaConfig>>()

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

        val transitionRefs =
            allResults.map { (config, transition, _) ->
                TransitionRef(
                    fromStateIndex = config.stateIndex,
                    toStateIndex = transition.toState,
                    transitionIndex = transitions.indexOf(transition),
                )
            }

        // No-progress loop: same (state, offset) set — treeNodeId excluded from the key
        val newKeys = allResults.map { it.third.stateIndex to it.third.inputOffset }.toSet()
        val currentKeys = currentConfigs.map { it.stateIndex to it.inputOffset }.toSet()
        if (newKeys == currentKeys) {
            return terminateSimulation()
        }

        val minOffset = allResults.minOf { it.third.inputOffset }

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
            },
        )
    }

    private fun terminateSimulation(): Simulation.Ended {
        val acceptingConfigs =
            currentConfigs.filter { config ->
                getStateByIndex(config.stateIndex).final &&
                    config.inputOffset >= remainingInput.length
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

    private fun getMatchingTransitions(fromState: State, inputOffset: Int): List<Transition> {
        val remaining =
            if (inputOffset < remainingInput.length) {
                remainingInput.substring(inputOffset)
            } else {
                ""
            }
        return transitions.filter {
            it.fromState == fromState.index && (it.read.isEmpty() || remaining.startsWith(it.read))
        }
    }
}
