package com.sfag.automata.domain.simulation

sealed interface NodeSnapshot {
    val inputConsumed: Int

    data class FaSnapshot(override val inputConsumed: Int) : NodeSnapshot

    data class PdaSnapshot(override val inputConsumed: Int, val stack: List<Char>) : NodeSnapshot

    data class TmSnapshot(val tape: List<Char>, val headPosition: Int) : NodeSnapshot {
        override val inputConsumed: Int
            get() = 0
    }
}
