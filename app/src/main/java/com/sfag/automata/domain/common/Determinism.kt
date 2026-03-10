package com.sfag.automata.domain.common

import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.main.config.Symbols.isEpsilonLabel

fun Machine.determinismLabel(): String? {
    val det = checkDeterminism() ?: return null
    val prefix = if (det) "D" else "N"
    return "$prefix$typeLabel"
}

fun Machine.checkDeterminism(): Boolean? =
    when (this) {
        is FiniteMachine -> checkFiniteDeterminism()
        is PushdownMachine -> checkPushdownDeterminism()
        is TuringMachine -> checkTuringDeterminism()
    }

private fun FiniteMachine.checkFiniteDeterminism(): Boolean? {
    if (states.isEmpty() && transitions.isEmpty()) return null
    if (states.none { it.initial }) return null
    if (states.count { it.initial } != 1) return false
    val seenLabels = mutableSetOf<Pair<Int, String>>()
    for (transition in transitions) {
        val label = transition.name.trim()
        if (label.isEmpty() || isEpsilonLabel(label)) return false
        if (!seenLabels.add(transition.fromState to label)) return false
    }
    return true
}

private fun PushdownMachine.checkPushdownDeterminism(): Boolean? {
    if (states.isEmpty() && transitions.isEmpty()) return null
    if (states.none { it.initial }) return null
    if (states.count { it.initial } != 1) return false
    val seenLabels = mutableSetOf<Triple<Int, String, String>>()
    val epsilonPairs = mutableSetOf<Pair<Int, String>>()
    val nonEpsilonPairs = mutableSetOf<Pair<Int, String>>()
    for (transition in pdaTransitions) {
        val input = transition.name.trim()
        val pop = transition.pop
        if (!seenLabels.add(Triple(transition.fromState, input, pop))) return false
        val statePop = transition.fromState to pop
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

private fun TuringMachine.checkTuringDeterminism(): Boolean? {
    if (states.isEmpty() && transitions.isEmpty()) return null
    if (states.none { it.initial }) return null
    if (states.count { it.initial } != 1) return false
    val seenLabels = mutableSetOf<Pair<Int, String>>()
    for (transition in turingTransitions) {
        val readSymbol = transition.name.trim()
        if (!seenLabels.add(transition.fromState to readSymbol)) return false
    }
    return true
}
