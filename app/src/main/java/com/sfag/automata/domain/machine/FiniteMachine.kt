package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef

private const val MAX_REACHABILITY_CONFIGS = 100_000

data class FaConfig(val stateIndex: Int, val inputOffset: Int = 0)

class FiniteMachine(
    name: String = "",
    states: MutableList<State> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    override val transitions: MutableList<Transition> = mutableListOf(),
) : Machine(name, states, savedInputs = savedInputs) {
    override val jffTag = "fa"
    override val typeLabel = "FA"

    // Track multiple current configs for NFA simulation (with per-config input offset)
    val currentConfigs: MutableSet<FaConfig> = mutableSetOf()

    // Delegates to currentConfigs
    override var currentState: Int?
        get() = currentConfigs.firstOrNull()?.stateIndex
        set(value) {
            currentConfigs.clear()
            value?.let { currentConfigs.add(FaConfig(it)) }
        }

    override fun advanceSimulation(): Simulation {
        if (currentConfigs.isEmpty()) {
            if (!ensureCurrentState()) return Simulation.Ended(SimulationOutcome.ACTIVE)
            getStateByIndexOrNull(currentState!!)?.isCurrent = true
        }
        return calculateAllPathsStep()
    }

    private fun calculateAllPathsStep(): Simulation {
        val epsilonTransitions = mutableListOf<Pair<FaConfig, Transition>>()
        val inputTransitions = mutableListOf<Pair<FaConfig, Transition>>()

        for (config in currentConfigs) {
            val state = getStateByIndex(config.stateIndex)
            for (transition in getMatchingTransitions(state, config.inputOffset)) {
                if (transition.name.isEmpty()) {
                    epsilonTransitions.add(config to transition)
                } else {
                    inputTransitions.add(config to transition)
                }
            }
        }

        // Process epsilon transitions first: add reachable configs that aren't already current
        if (epsilonTransitions.isNotEmpty()) {
            val newConfigs =
                epsilonTransitions
                    .map { (config, transition) ->
                        FaConfig(
                            transition.toState,
                            config.inputOffset
                        )
                    }
                    .filter { it !in currentConfigs }
                    .toSet()
            if (newConfigs.isNotEmpty()) {
                val transitionRefs =
                    epsilonTransitions
                        .filter { (config, transition) ->
                            FaConfig(transition.toState, config.inputOffset) in newConfigs
                        }
                        .map { TransitionRef(it.first.stateIndex, it.second.toState) }
                        .distinct()

                expandSimulationTree(transitionRefs, keepActive = true)

                return Simulation.Step(
                    transitionRefs = transitionRefs,
                    onAllComplete = {
                        for (config in newConfigs) {
                            currentConfigs.add(config)
                            getStateByIndexOrNull(config.stateIndex)?.isCurrent = true
                        }
                    },
                )
            }
        }

        // No input transitions available - check acceptance
        if (inputTransitions.isEmpty()) {
            val anyAccepting =
                currentConfigs.any { config ->
                    getStateByIndex(config.stateIndex).final &&
                            config.inputOffset >= remainingInput.length
                }
            if (anyAccepting) {
                val acceptedIds =
                    tree
                        .getActiveNodes()
                        .filter { node -> states.any { it.name == node.stateName && it.final } }
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
            inputTransitions
                .map { (config, transition) ->
                    TransitionRef(
                        fromStateIndex = config.stateIndex,
                        toStateIndex = transition.toState,
                    )
                }
                .distinct()

        val newConfigs =
            inputTransitions
                .map { (config, transition) ->
                    FaConfig(transition.toState, config.inputOffset + transition.name.length)
                }
                .toSet()

        expandSimulationTree(transitionRefs)

        // Advance shared input by minimum new offset
        val minOffset = newConfigs.minOf { it.inputOffset }
        consumeInput(minOffset)

        // Adjust offsets relative to new input position
        val adjustedConfigs =
            newConfigs.map { FaConfig(it.stateIndex, it.inputOffset - minOffset) }.toSet()

        return Simulation.Step(
            transitionRefs = transitionRefs,
            onAllComplete = {
                for (config in currentConfigs) {
                    getStateByIndexOrNull(config.stateIndex)?.isCurrent = false
                }
                currentConfigs.clear()
                for (config in adjustedConfigs) {
                    currentConfigs.add(config)
                    getStateByIndexOrNull(config.stateIndex)?.isCurrent = true
                }
            },
        )
    }

    private fun consumeInput(length: Int) {
        if (length > 0 && remainingInput.isNotEmpty()) {
            remainingInput.delete(0, minOf(length, remainingInput.length))
        }
    }

    override fun resetSimulation() {
        val initialStateIndex = states.firstOrNull { it.initial }?.index
        for (state in states) {
            if (state.index != initialStateIndex) {
                state.isCurrent = false
            }
        }
        currentConfigs.clear()
        initialStateIndex?.let { currentConfigs.add(FaConfig(it)) }
    }

    override fun removeTransition(transition: Transition) {
        transitions.remove(transition)
    }

    override fun addNewState(state: State) {
        if (state.initial && currentConfigs.isEmpty()) {
            currentConfigs.add(FaConfig(state.index))
            state.isCurrent = true
        }
        states.add(state)
    }

    fun addNewTransition(
        name: String,
        fromState: State,
        toState: State,
    ) {
        val alreadyExists =
            transitions.any { transition ->
                transition.fromState == fromState.index && transition.toState == toState.index && transition.name == name
            }
        if (!alreadyExists) {
            transitions.add(FiniteTransition(name, fromState.index, toState.index))
        }
    }

    override fun canReachFinalState(
        input: StringBuilder,
        fromInit: Boolean,
    ): Boolean? {
        data class BfsPath(
            val stateIndex: Int,
            val inputIndex: Int,
        )

        val startIndex = findStartStateIndex(fromInit) ?: return false
        val visited = mutableSetOf<Pair<Int, Int>>()
        var paths = mutableListOf(BfsPath(startIndex, 0))
        val maxConfigs = MAX_REACHABILITY_CONFIGS

        while (paths.isNotEmpty() && visited.size < maxConfigs) {
            val nextPaths = mutableListOf<BfsPath>()

            for (path in paths) {
                if (!visited.add(path.stateIndex to path.inputIndex)) continue
                if (visited.size > maxConfigs) break
                val state = getStateByIndexOrNull(path.stateIndex) ?: continue
                if (path.inputIndex == input.length && state.final) return true

                val remaining = input.substring(path.inputIndex)
                val possibleTransitions =
                    if (path.inputIndex < input.length) {
                        transitions.filter {
                            it.fromState == path.stateIndex &&
                                    (it.name.isEmpty() || remaining.startsWith(it.name))
                        }
                    } else {
                        transitions.filter { it.fromState == path.stateIndex && it.name.isEmpty() }
                    }

                for (transition in possibleTransitions) {
                    nextPaths.add(
                        BfsPath(transition.toState, path.inputIndex + transition.name.length),
                    )
                }
            }

            paths = nextPaths
        }

        return if (paths.isEmpty()) false else null
    }

    private fun getMatchingTransitions(fromState: State, inputOffset: Int): List<Transition> {
        val remaining =
            if (inputOffset < remainingInput.length) {
                remainingInput.substring(inputOffset)
            } else {
                ""
            }
        return transitions.filter {
            it.fromState == fromState.index &&
                    (it.name.isEmpty() || remaining.startsWith(it.name))
        }
    }
}
