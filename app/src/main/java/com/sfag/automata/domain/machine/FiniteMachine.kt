package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef

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
            if (!ensureCurrentState()) {
                return Simulation.Ended(SimulationOutcome.ACTIVE)
            }
            getStateByIndexOrNull(currentState!!)?.isCurrent = true
        }
        return calculateAllPathsStep()
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

    fun addNewTransition(read: String, fromState: State, toState: State) {
        val alreadyExists =
            transitions.any { transition ->
                transition.fromState == fromState.index &&
                    transition.toState == toState.index &&
                    transition.read == read
            }
        if (!alreadyExists) {
            transitions.add(FiniteTransition(fromState.index, toState.index, read))
        }
    }

    private fun calculateAllPathsStep(): Simulation {
        val allTransitions = mutableListOf<Pair<FaConfig, Transition>>()

        for (config in currentConfigs) {
            val state = getStateByIndex(config.stateIndex)
            for (transition in getMatchingTransitions(state, config.inputOffset)) {
                allTransitions.add(config to transition)
            }
        }

        // No transitions available - check acceptance
        if (allTransitions.isEmpty()) {
            val acceptingConfigs =
                currentConfigs.filter { config ->
                    getStateByIndex(config.stateIndex).final &&
                        config.inputOffset >= remainingInput.length
                }
            val anyAccepting = acceptingConfigs.isNotEmpty()
            return Simulation.Ended(
                outcome =
                    if (anyAccepting) SimulationOutcome.ACCEPTED else SimulationOutcome.REJECTED,
                isNodeAccepting =
                    if (anyAccepting)
                        { node ->
                            val state = states.firstOrNull { it.name == node.stateName }
                            state != null && acceptingConfigs.any { it.stateIndex == state.index }
                        }
                    else null,
            )
        }

        // Fire all transitions (epsilon + input) simultaneously
        val transitionRefs =
            allTransitions.map { (config, transition) ->
                TransitionRef(
                    fromStateIndex = config.stateIndex,
                    toStateIndex = transition.toState,
                    transitionIndex = transitions.indexOf(transition),
                )
            }

        val newConfigs =
            allTransitions
                .map { (config, transition) ->
                    FaConfig(transition.toState, config.inputOffset + transition.read.length)
                }
                .toSet()

        // Detect no-progress loop (e.g. q0 -e-> q1 -e-> q0 cycling)
        if (newConfigs == currentConfigs.toSet()) {
            val acceptingConfigs =
                currentConfigs.filter { config ->
                    getStateByIndex(config.stateIndex).final &&
                        config.inputOffset >= remainingInput.length
                }
            val anyAccepting = acceptingConfigs.isNotEmpty()
            return Simulation.Ended(
                outcome =
                    if (anyAccepting) SimulationOutcome.ACCEPTED else SimulationOutcome.REJECTED,
                isNodeAccepting =
                    if (anyAccepting)
                        { node ->
                            val state = states.firstOrNull { it.name == node.stateName }
                            state != null && acceptingConfigs.any { it.stateIndex == state.index }
                        }
                    else null,
            )
        }

        // Consume input now so the tape head advances at animation start
        val minOffset = newConfigs.minOf { it.inputOffset }
        consumeInput(minOffset)
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
            it.fromState == fromState.index && (it.read.isEmpty() || remaining.startsWith(it.read))
        }
    }
}
