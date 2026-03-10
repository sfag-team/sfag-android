package com.sfag.automata.domain.common

import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.main.config.Symbols

/** Machine formal definition for display. */
data class FormalDefinition(
    val stateNames: List<String>,
    val inputAlphabet: Set<Char>,
    val initialStateName: String,
    val finalStateNames: List<String>,
    val transitionDescriptions: List<String>,
    // PDA-specific fields
    val stackAlphabet: Set<Char>? = null,
    val initialStackSymbol: Char? = null,
    // Turing-specific fields
    val tapeAlphabet: Set<Char>? = null,
    val blankSymbol: Char? = null,
)

fun Machine.getFormalDefinition(): FormalDefinition =
    when (this) {
        is FiniteMachine -> getFiniteFormalDefinition()
        is PushdownMachine -> getPushdownFormalDefinition()
        is TuringMachine -> getTuringFormalDefinition()
    }

private fun FiniteMachine.getFiniteFormalDefinition(): FormalDefinition {
    val transitionDescriptions =
        transitions.map { transition ->
            val fromStateName = getStateByIndexOrNull(transition.fromState)?.name ?: "?"
            val toStateName = getStateByIndexOrNull(transition.toState)?.name ?: "?"
            val readSymbol = transition.name.ifEmpty { Symbols.EPSILON }
            "${Symbols.DELTA}($fromStateName, $readSymbol) = $toStateName"
        }

    return FormalDefinition(
        stateNames = states.map { it.name },
        inputAlphabet = transitions.mapNotNull { it.name.firstOrNull() }.toSet(),
        initialStateName = states.firstOrNull { it.initial }?.name ?: "q0",
        finalStateNames = states.filter { it.final }.map { it.name },
        transitionDescriptions = transitionDescriptions,
    )
}

private fun PushdownMachine.getPushdownFormalDefinition(): FormalDefinition {
    val transitionDescriptions =
        pdaTransitions.map { transition ->
            val fromStateName = getStateByIndexOrNull(transition.fromState)?.name ?: "?"
            val toStateName = getStateByIndexOrNull(transition.toState)?.name ?: "?"
            val readSymbol = transition.name.ifEmpty { Symbols.EPSILON }
            val popSymbol = transition.pop.ifEmpty { Symbols.EPSILON }
            val pushSymbol = transition.push.ifEmpty { Symbols.EPSILON }
            "${Symbols.DELTA}($fromStateName, $readSymbol, $popSymbol) = ($toStateName, $pushSymbol)"
        }

    val stackAlphabetSet =
        pdaTransitions.flatMap { (it.pop + it.push).toCharArray().toList() }.toSet().plus('Z')

    return FormalDefinition(
        stateNames = states.map { it.name },
        inputAlphabet = transitions.mapNotNull { it.name.firstOrNull() }.toSet(),
        initialStateName = states.firstOrNull { it.initial }?.name ?: "q0",
        finalStateNames = states.filter { it.final }.map { it.name },
        transitionDescriptions = transitionDescriptions,
        stackAlphabet = stackAlphabetSet,
        initialStackSymbol = 'Z',
    )
}

private fun TuringMachine.getTuringFormalDefinition(): FormalDefinition {
    val transitionDescriptions =
        turingTransitions.map { transition ->
            val fromStateName = getStateByIndexOrNull(transition.fromState)?.name ?: "?"
            val toStateName = getStateByIndexOrNull(transition.toState)?.name ?: "?"
            val readSymbol = transition.name.ifEmpty { Symbols.EPSILON }
            "${Symbols.DELTA}($fromStateName, $readSymbol) = ($toStateName, ${transition.writeSymbol}, ${transition.direction})"
        }

    val tapeAlphabetSet =
        turingTransitions
            .flatMap { listOf(it.name.firstOrNull(), it.writeSymbol) }
            .filterNotNull()
            .toSet()
            .plus(blankSymbol)

    return FormalDefinition(
        stateNames = states.map { it.name },
        inputAlphabet = transitions.mapNotNull { it.name.firstOrNull() }.toSet(),
        initialStateName = states.firstOrNull { it.initial }?.name ?: "q0",
        finalStateNames = states.filter { it.final }.map { it.name },
        transitionDescriptions = transitionDescriptions,
        tapeAlphabet = tapeAlphabetSet,
        blankSymbol = blankSymbol,
    )
}
