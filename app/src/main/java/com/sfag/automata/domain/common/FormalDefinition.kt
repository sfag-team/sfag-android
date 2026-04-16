package com.sfag.automata.domain.common

import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.main.config.Symbols

/** Machine formal definition for display. */
sealed class FormalDefinition {
    abstract val stateNames: List<String>
    abstract val inputAlphabet: Set<Char>
    abstract val initialStateName: String
    abstract val finalStateNames: List<String>
    abstract val transitionLabels: List<String>

    data class Finite(
        override val stateNames: List<String>,
        override val inputAlphabet: Set<Char>,
        override val initialStateName: String,
        override val finalStateNames: List<String>,
        override val transitionLabels: List<String>,
    ) : FormalDefinition()

    data class Pushdown(
        override val stateNames: List<String>,
        override val inputAlphabet: Set<Char>,
        override val initialStateName: String,
        override val finalStateNames: List<String>,
        override val transitionLabels: List<String>,
        val stackAlphabet: Set<Char>,
        val initialStackSymbol: Char,
    ) : FormalDefinition()

    data class Turing(
        override val stateNames: List<String>,
        override val inputAlphabet: Set<Char>,
        override val initialStateName: String,
        override val finalStateNames: List<String>,
        override val transitionLabels: List<String>,
        val tapeAlphabet: Set<Char>,
        val blankSymbol: Char,
    ) : FormalDefinition()
}

fun Machine.getFormalDefinition(): FormalDefinition {
    val stateNames = states.map { it.name }
    val inputAlphabet = transitions.mapNotNull { it.read.firstOrNull() }.toSet()
    val initialStateName = initialState?.name.orEmpty()
    val finalStateNames = states.filter { it.final }.map { it.name }
    return when (this) {
        is FiniteMachine ->
            FormalDefinition.Finite(
                stateNames,
                inputAlphabet,
                initialStateName,
                finalStateNames,
                transitionLabels(),
            )

        is PushdownMachine ->
            FormalDefinition.Pushdown(
                stateNames,
                inputAlphabet,
                initialStateName,
                finalStateNames,
                transitionLabels(),
                stackAlphabet = stackAlphabet(),
                initialStackSymbol = Symbols.INITIAL_STACK_SYMBOL,
            )

        is TuringMachine ->
            FormalDefinition.Turing(
                stateNames,
                inputAlphabet,
                initialStateName,
                finalStateNames,
                transitionLabels(),
                tapeAlphabet = tapeAlphabet(),
                blankSymbol = blankSymbol,
            )
    }
}

private fun FiniteMachine.transitionLabels(): List<String> =
    transitions.map { transition ->
        val fromStateName = getStateByIndex(transition.fromState).name
        val toStateName = getStateByIndex(transition.toState).name
        val readSymbol = transition.read.ifEmpty { Symbols.EPSILON }
        "${Symbols.DELTA}($fromStateName, $readSymbol) = $toStateName"
    }

private fun PushdownMachine.transitionLabels(): List<String> =
    pdaTransitions.map { transition ->
        val fromStateName = getStateByIndex(transition.fromState).name
        val toStateName = getStateByIndex(transition.toState).name
        val readSymbol = transition.read.ifEmpty { Symbols.EPSILON }
        val popSymbol = transition.pop.ifEmpty { Symbols.EPSILON }
        val pushSymbol = transition.push.ifEmpty { Symbols.EPSILON }
        "${Symbols.DELTA}($fromStateName, $readSymbol, $popSymbol) = ($toStateName, $pushSymbol)"
    }

private fun TuringMachine.transitionLabels(): List<String> =
    tmTransitions.map { transition ->
        val fromStateName = getStateByIndex(transition.fromState).name
        val toStateName = getStateByIndex(transition.toState).name
        val readSymbol = transition.read.ifEmpty { Symbols.EPSILON }
        "${Symbols.DELTA}($fromStateName, $readSymbol) = ($toStateName, ${transition.write}, ${transition.direction})"
    }

private fun PushdownMachine.stackAlphabet(): Set<Char> =
    pdaTransitions.flatMap { (it.pop + it.push).toList() }.toSet() + Symbols.INITIAL_STACK_SYMBOL

private fun TuringMachine.tapeAlphabet(): Set<Char> =
    tmTransitions.flatMap { listOfNotNull(it.read.firstOrNull(), it.write) }.toSet() + blankSymbol
