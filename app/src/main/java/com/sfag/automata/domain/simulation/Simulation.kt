package com.sfag.automata.domain.simulation

import com.sfag.automata.domain.tree.Branch
import com.sfag.automata.domain.tree.TreeNode

/**
 * Identifies a single transition to animate. Uses state indices so the domain layer stays free of
 * Compose/position types.
 */
data class TransitionRef(
    val fromStateIndex: Int,
    val toStateIndex: Int,
    val transitionIndex: Int = -1,
)

/**
 * Result of a simulation step calculation. Domain layer returns this data, UI layer handles
 * animation and derivation tree updates.
 */
sealed class Simulation {
    /** No transition available - simulation ended. */
    data class Ended(
        val outcome: SimulationOutcome,
        val isNodeAccepting: ((TreeNode) -> Boolean)? = null,
    ) : Simulation()

    /**
     * One or more transitions found - animations play simultaneously. Covers both deterministic (1
     * transition) and NFA (n transitions) steps uniformly.
     */
    class Step(
        val transitionRefs: List<TransitionRef>,
        val treeBranches: Map<Int, List<Branch>>,
        val onAllComplete: () -> Unit,
    ) : Simulation()
}
