package com.sfag.automata.domain.simulation

import com.sfag.automata.domain.tree.Branch

/**
 * Immutable snapshot of simulation state at one step. Frame 0 is the initial state; subsequent
 * frames each represent the state after one step. The last frame carries the terminal status and an
 * `isNodeAccepting` predicate over treeNodeIds when the simulation has ended.
 */
data class SimFrame(
    val transitionRefs: List<TransitionRef>,
    val treeBranches: Map<Int, List<Branch>>,
    val activeConfigs: Map<Int, SimConfig>,
    val status: SimStatus,
    val isNodeAccepting: ((Int) -> Boolean)? = null,
) {
    val activeStateIndices: Set<Int> = activeConfigs.values.mapTo(mutableSetOf()) { it.stateIndex }

    val isTerminal: Boolean = status != SimStatus.ACTIVE
}
