package com.sfag.automata.domain.common

import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.Transition
import com.sfag.automata.domain.machine.TuringMachine

fun Machine.determinismLabel(): String? {
    return when (isDeterministic()) {
        true -> "D$typeLabel"
        false -> "N$typeLabel"
        null -> null
    }
}

fun Machine.isDeterministic(): Boolean? {
    if (states.isEmpty() && transitions.isEmpty()) {
        return null
    }
    if (states.none { it.initial }) {
        return null
    }
    if (states.count { it.initial } != 1) {
        return false
    }
    return when (this) {
        is FiniteMachine -> {
            if (transitions.any { it.read.isEmpty() }) {
                return false
            }
            !transitions.hasPairwiseConflict { t1, t2 ->
                t1.read.startsWith(t2.read) || t2.read.startsWith(t1.read)
            }
        }

        is PushdownMachine ->
            !pdaTransitions.hasPairwiseConflict { t1, t2 ->
                val inputOverlap =
                    t1.read.isEmpty() ||
                        t2.read.isEmpty() ||
                        t1.read.startsWith(t2.read) ||
                        t2.read.startsWith(t1.read)
                val popOverlap =
                    t1.pop.isEmpty() ||
                        t2.pop.isEmpty() ||
                        t1.pop.startsWith(t2.pop) ||
                        t2.pop.startsWith(t1.pop)
                inputOverlap && popOverlap
            }

        is TuringMachine ->
            !tmTransitions.hasPairwiseConflict { t1, t2 ->
                t1.read.startsWith(t2.read) || t2.read.startsWith(t1.read)
            }
    }
}

private fun <T : Transition> List<T>.hasPairwiseConflict(conflicts: (T, T) -> Boolean): Boolean {
    val byState = groupBy { it.fromState }
    for ((_, group) in byState) {
        for (i in group.indices) {
            for (j in i + 1 until group.size) {
                if (conflicts(group[i], group[j])) {
                    return true
                }
            }
        }
    }
    return false
}
