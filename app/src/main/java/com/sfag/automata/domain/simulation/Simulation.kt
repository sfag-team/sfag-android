package com.sfag.automata.domain.simulation

/**
 * Identifies a single transition to animate. Uses state indices so the domain layer stays free of
 * Compose/position types.
 */
data class TransitionRef(
    val fromStateIndex: Int,
    val toStateIndex: Int,
)

/**
 * Result of a simulation step calculation. Domain layer returns this data, UI layer handles
 * animation.
 */
sealed class Simulation {
    /** No transition available - simulation ended. */
    data class Ended(
        val outcome: SimulationOutcome,
    ) : Simulation()

    /**
     * One or more transitions found - animations play simultaneously. Covers both deterministic (1
     * transition) and NFA (n transitions) steps uniformly.
     */
    class Step(
        val transitionRefs: List<TransitionRef>,
        val onAllComplete: () -> Unit,
    ) : Simulation()
}
