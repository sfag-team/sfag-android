package com.sfag.automata.domain.simulation

import com.sfag.automata.domain.machine.Transition

/**
 * One active simulation branch's state. Mutated only via Machine subclasses' onAllComplete;
 * immutable after a step ends, so the same instance can also be the per-frame snapshot the UI
 * consumes from [SimFrame.activeConfigs].
 *
 * `inputConsumed` is absolute against `Machine.fullInput` (not relative to remaining input), so a
 * stored config remains meaningful even after subsequent steps consume more input.
 */
sealed interface SimConfig {
    val stateIndex: Int
    val treeNodeId: Int

    /**
     * Returns this config with [treeNodeId] replaced - one of the few common ops across subtypes.
     */
    fun withTreeNodeId(id: Int): SimConfig

    data class Fa(
        override val stateIndex: Int,
        val inputConsumed: Int = 0,
        override val treeNodeId: Int = -1,
    ) : SimConfig {
        override fun withTreeNodeId(id: Int): Fa = copy(treeNodeId = id)
    }

    data class Pda(
        override val stateIndex: Int,
        val stack: List<Char>,
        val inputConsumed: Int = 0,
        override val treeNodeId: Int = -1,
    ) : SimConfig {
        override fun withTreeNodeId(id: Int): Pda = copy(treeNodeId = id)
    }

    data class Tm(
        override val stateIndex: Int,
        val tape: List<Char>,
        val headPosition: Int,
        override val treeNodeId: Int = -1,
    ) : SimConfig {
        override fun withTreeNodeId(id: Int): Tm = copy(treeNodeId = id)
    }
}

/** One transition application: source config, destination config, and the transition taken. */
data class StepResult<C : SimConfig>(val src: C, val dest: C, val transition: Transition)
