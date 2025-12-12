package com.sfag.automata.domain.usecase.validation

import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.shared.util.Symbols

/**
 * Checks whether the given finite automaton is deterministic (DFA).
 *
 * Conditions:
 *  - machine is of type Finite
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
