package com.sfag.automata.domain.simulation

import com.sfag.automata.domain.tree.Branch

/**
 * Identifies a single transition to animate. Uses state indices so the domain layer stays free of
 * Compose/position types.
 */
data class TransitionRef(val fromStateIndex: Int, val toStateIndex: Int, val transitionIndex: Int)

/** Result of a simulation step calculation. */
sealed class Simulation {
    /** No transition available - simulation ended. `isNodeAccepting` is set on accepting end. */
    data class Ended(
        val outcome: SimulationOutcome,
        val isNodeAccepting: ((Int) -> Boolean)? = null,
    ) : Simulation()

    /** One or more transitions found. Covers deterministic and non-deterministic steps. */
    class Step(
        val transitionRefs: List<TransitionRef>,
        val treeBranches: Map<Int, List<Branch>>,
        val onAllComplete: () -> Unit,
    ) : Simulation()
}
