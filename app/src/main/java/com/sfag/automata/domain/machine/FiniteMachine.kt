package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef
import com.sfag.main.config.MAX_FA_PDA_CONFIGS

data class FaConfig(val stateIndex: Int, val inputOffset: Int = 0)

class FiniteMachine(
    name: String = "",
    states: MutableList<State> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    override val transitions: MutableList<Transition> = mutableListOf(),
) : Machine(name, states, savedInputs = savedInputs) {
    override val jffTag = "fa"
    override val typeLabel = "FA"

    // Track multiple current configs for NFA simulation
    val currentConfigs: MutableSet<FaConfig> = mutableSetOf()

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

    override fun canReachFinalState(
        input: StringBuilder,
        fromInit: Boolean,
    ): Boolean? {
        data class Config(val stateIndex: Int, val inputIndex: Int)

        val startIndex = findStartStateIndex(fromInit) ?: return false
        return bfsReachability(
            initialConfigs = listOf(Config(startIndex, 0)),
            expand = { config ->
                val remaining = input.substring(minOf(config.inputIndex, input.length))
                val possibleTransitions =
                    if (config.inputIndex < input.length) {
                        transitions.filter {
                            it.fromState == config.stateIndex &&
                                    (it.name.isEmpty() || remaining.startsWith(it.name))
                        }
                    } else {
                        transitions.filter { it.fromState == config.stateIndex && it.name.isEmpty() }
                    }
                possibleTransitions.map { Config(it.toState, config.inputIndex + it.name.length) }
            },
            isAccepted = { config ->
                val state = getStateByIndexOrNull(config.stateIndex)
                config.inputIndex == input.length && state?.final == true
            },
            maxConfigs = MAX_FA_PDA_CONFIGS,
        )
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

    override fun resetSimulation() {
        super.resetSimulation()
        currentConfigs.clear()
        states.firstOrNull { it.initial }?.index?.let { currentConfigs.add(FaConfig(it)) }
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
                        .map {
                            TransitionRef(
                                it.first.stateIndex,
                                it.second.toState,
                                transitions.indexOf(it.second),
                            )
                        }

                return Simulation.Step(
                    transitionRefs = transitionRefs,
                    keepActive = true,
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
            val acceptingConfigs =
                currentConfigs.filter { config ->
                    getStateByIndex(config.stateIndex).final &&
                            config.inputOffset >= remainingInput.length
                }
            val anyAccepting = acceptingConfigs.isNotEmpty()
            return Simulation.Ended(
                outcome = if (anyAccepting) SimulationOutcome.ACCEPTED else SimulationOutcome.REJECTED,
                isNodeAccepting = if (anyAccepting) { node ->
                    val state = states.firstOrNull { it.name == node.stateName }
                    state != null && acceptingConfigs.any { it.stateIndex == state.index }
                } else null,
            )
        }

        // Process ALL input transitions - fire all lengths simultaneously
        val transitionRefs =
            inputTransitions
                .map { (config, transition) ->
                    TransitionRef(
                        fromStateIndex = config.stateIndex,
                        toStateIndex = transition.toState,
                        transitionIndex = transitions.indexOf(transition),
                    )
                }

        val newConfigs =
            inputTransitions
                .map { (config, transition) ->
                    FaConfig(transition.toState, config.inputOffset + transition.name.length)
                }
                .toSet()

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
