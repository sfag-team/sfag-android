package com.sfag.automata.domain.usecase.simulation

import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.SimulationState
import com.sfag.automata.domain.model.transition.Transition

/**
 * Interface for simulation engines that execute automata.
 * Each machine type has its own simulator implementation.
 */
interface SimulationEngine {
    /**
     * Initialize simulation with input string.
     * @return Initial simulation state
     */
    fun initialize(machine: Machine, input: String): SimulationState

    /**
     * Execute one step of the simulation.
     * @return New simulation state after the step, and the transition taken (null if none)
     */
    fun step(machine: Machine, state: SimulationState): Pair<SimulationState, Transition?>

    /**
     * Check if a final state can be reached from current state.
     * Used for choosing the best path in non-deterministic automata.
     */
    fun canReachFinalState(machine: Machine, state: SimulationState): Boolean

    /**
     * Get all available transitions from current state.
     */
    fun getAvailableTransitions(machine: Machine, state: SimulationState): List<Transition>

    /**
     * Reset simulation to initial state.
     */
    fun reset(machine: Machine, state: SimulationState): SimulationState
}
