package com.sfag.automata.domain.model.simulation

/**
 * Result of a simulation step calculation.
 * Domain layer returns this data, UI layer handles animation.
 */
sealed class SimulationResult {
    /**
     * No transition available - simulation ended.
     * @param accepted true if in accepting state, false if rejected, null if incomplete
     */
    data class Ended(val accepted: Boolean?) : SimulationResult()

    /**
     * One or more transitions found - animations play simultaneously.
     * Covers both deterministic (1 transition) and NFA (n transitions) steps uniformly.
     * Not a data class: holds a lambda so equals/copy/toString would be misleading.
     * @param transitions list of transition data for animation (size >= 1)
     * @param onAllComplete callback to execute after all animations complete
     */
    class Step(
        val transitions: List<TransitionData>,
        val onAllComplete: () -> Unit
    ) : SimulationResult()
}

/**
 * Data for a single transition animation.
 * Uses state indices so the domain layer stays free of Compose/position types.
 */
data class TransitionData(
    val startStateIndex: Int,
    val endStateIndex: Int,
)
