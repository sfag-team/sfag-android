package com.sfag.automata.domain.simulation

import com.sfag.automata.domain.machine.Config
import com.sfag.automata.domain.tree.Branch

/**
 * Immutable snapshot of simulation state at one step. Frame 0 is the initial state; subsequent
 * frames each represent the state after one step. The last frame carries the simulation outcome and
 * an `isNodeAccepting` predicate over treeNodeIds when the simulation has ended.
 */
data class MachineFrame(
    val transitionRefs: List<TransitionRef>,
    val treeBranches: Map<Int, List<Branch>>,
    val activeConfigs: Map<Int, Config>,
    val outcome: SimulationOutcome,
    val isNodeAccepting: ((Int) -> Boolean)? = null,
) {
    val activeStateIndices: Set<Int> = activeConfigs.values.mapTo(mutableSetOf()) { it.stateIndex }

    val isTerminal: Boolean = outcome != SimulationOutcome.ACTIVE
}
