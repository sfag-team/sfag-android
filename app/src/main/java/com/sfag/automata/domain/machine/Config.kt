package com.sfag.automata.domain.machine

/**
 * One active simulation path. Mutated only via Machine subclasses' onAllComplete; immutable after a
 * step ends, so the same instance can also be the per-frame snapshot the UI consumes from
 * `MachineFrame.activeConfigs`.
 *
 * `inputConsumed` is absolute against `Machine.fullInput` (not relative to remaining input), so a
 * stored config remains meaningful even after subsequent steps consume more input.
 */
sealed interface Config {
    val stateIndex: Int
    val treeNodeId: Int

    data class Fa(
        override val stateIndex: Int,
        val inputConsumed: Int = 0,
        override val treeNodeId: Int = -1,
    ) : Config

    data class Pda(
        override val stateIndex: Int,
        val stack: List<Char>,
        val inputConsumed: Int = 0,
        override val treeNodeId: Int = -1,
    ) : Config

    data class Tm(
        override val stateIndex: Int,
        val tape: List<Char>,
        val headPosition: Int,
        override val treeNodeId: Int = -1,
    ) : Config
}

/** One transition application: source config, destination config, and the transition taken. */
data class StepResult<C : Config>(val src: C, val dest: C, val transition: Transition)
