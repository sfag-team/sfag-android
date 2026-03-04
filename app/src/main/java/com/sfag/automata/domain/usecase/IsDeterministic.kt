package com.sfag.automata.domain.usecase

import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.TuringTransition
import com.sfag.shared.util.Symbols

/**
 * Unified determinism check - dispatches to the appropriate check based on machine type.
 */
fun Machine.isDeterministic(): Boolean? {
    if (states.isEmpty() && transitions.isEmpty()) return null
    return when (machineType) {
        MachineType.Finite -> isDeterministicFinite()
        MachineType.Pushdown -> isDeterministicPushdown()
        MachineType.Turing -> isDeterministicTuring()
    }
}

/**
 * Checks whether the given finite automaton is deterministic (DFA).
 *
 * Conditions:
 *  - exactly one initial state
 *  - no epsilon transitions (empty label or epsilon symbol)
 *  - for each pair (startState, symbol) there is at most one transition
 */
fun Machine.isDeterministicFinite(): Boolean {
    if (machineType != MachineType.Finite) return false

    // 1) exactly one initial state
    if (states.count { it.initial } != 1) return false

    // 2) check for epsilon transitions and duplicate (startState, symbol) pairs
    val seen = mutableSetOf<Pair<Int, String>>()

    for (t in transitions) {
        val label = t.name.trim()

        // Empty label = epsilon transition -> non-deterministic
        if (label.isEmpty()) return false

        // Check for epsilon symbols in label
        if (isEpsilonLabel(label)) return false

        // For DFA, each transition label should be treated as a single symbol
        // Check if this (startState, label) pair already exists
        val key = t.startState to label
        if (!seen.add(key)) {
            // Duplicate transition from same state with same symbol
            return false
        }
    }

    return true
}

/**
 * Checks whether the given pushdown automaton is deterministic (DPDA).
 *
 * Conditions:
 *  - exactly one initial state
 *  - for each (startState, inputSymbol, popSymbol) triple: at most one transition
 *  - if an epsilon transition exists from state q with pop symbol X,
 *    then no other transition from q with pop X may exist for any input symbol
 */
private fun Machine.isDeterministicPushdown(): Boolean {
    if (machineType != MachineType.Pushdown) return false
    if (states.count { it.initial } != 1) return false

    val pdaTransitions = transitions.filterIsInstance<PushDownTransition>()

    // (startState, input, pop) -> must be unique
    val seen = mutableSetOf<Triple<Int, String, String>>()
    // (startState, pop) pairs that have an epsilon or non-epsilon transition
    val epsilonPairs = mutableSetOf<Pair<Int, String>>()
    val nonEpsilonPairs = mutableSetOf<Pair<Int, String>>()

    for (t in pdaTransitions) {
        val input = t.name.trim()
        val pop = t.pop

        val key = Triple(t.startState, input, pop)
        if (!seen.add(key)) return false

        val statePop = t.startState to pop
        // If epsilon transition exists for (state, pop), no other transition from that
        // state with same pop may exist - check both directions in a single pass
        if (input.isEmpty() || isEpsilonLabel(input)) {
            if (statePop in nonEpsilonPairs) return false
            epsilonPairs.add(statePop)
        } else {
            if (statePop in epsilonPairs) return false
            nonEpsilonPairs.add(statePop)
        }
    }

    return true
}

/**
 * Checks whether the given Turing machine is deterministic (DTM).
 *
 * Conditions:
 *  - exactly one initial state
 *  - for each (startState, readSymbol) pair: at most one transition
 */
private fun Machine.isDeterministicTuring(): Boolean {
    if (machineType != MachineType.Turing) return false
    if (states.count { it.initial } != 1) return false

    val seen = mutableSetOf<Pair<Int, String>>()

    for (t in transitions.filterIsInstance<TuringTransition>()) {
        val key = t.startState to t.name
        if (!seen.add(key)) return false
    }

    return true
}

/**
 * Checks if a label represents an epsilon transition.
 * Common epsilon representations: empty string, "eps", "epsilon", or Unicode epsilon
 */
private fun isEpsilonLabel(label: String): Boolean {
    val normalized = label.trim().lowercase()
    return normalized.isEmpty() ||
            normalized == "eps" ||
            normalized == "epsilon" ||
            normalized == Symbols.EPSILON ||
            normalized == Symbols.LAMBDA
}
