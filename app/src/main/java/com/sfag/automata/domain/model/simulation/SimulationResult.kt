package com.sfag.automata.domain.model.simulation

import androidx.compose.ui.geometry.Offset

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
     * Transition found - animation should play.
     * @param startPosition start state position for animation
     * @param endPosition end state position for animation
     * @param radius state radius for animation
     * @param onComplete callback to execute after animation completes
     */
    data class Transition(
        val startPosition: Offset,
        val endPosition: Offset,
        val radius: Float,
        val onComplete: () -> Unit
    ) : SimulationResult()

    /**
     * Multiple transitions found (NFA) - all animations should play simultaneously.
     * @param transitions list of transition data for parallel animation
     * @param onAllComplete callback to execute after all animations complete
     */
    data class MultipleTransitions(
        val transitions: List<TransitionData>,
        val onAllComplete: () -> Unit
    ) : SimulationResult()
}

/**
 * Data for a single transition animation in NFA simulation.
 */
data class TransitionData(
    val startPosition: Offset,
    val endPosition: Offset,
    val radius: Float
)
