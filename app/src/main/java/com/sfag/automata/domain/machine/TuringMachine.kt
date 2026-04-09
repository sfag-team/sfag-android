package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef
import com.sfag.main.config.Symbols

/**
 * A Turing machine consists of:
 * - A finite set of states Q
 * - A tape alphabet Γ (including blank symbol)
 * - An input alphabet Σ ⊆ Γ
 * - A transition function δ: Q × Γ -> Q × Γ × {L, R, S}
 * - An initial state q0
 * - A blank symbol (default '␣')
 * - A set of accepting/final states F
 */
class TuringMachine(
    name: String = "",
    states: MutableList<State> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    val turingTransitions: MutableList<TuringTransition> = mutableListOf(),
    val blankSymbol: Char = Symbols.BLANK_CHAR,
) : Machine(name, states, savedInputs = savedInputs) {
    override val jffTag = "turing"
    override val typeLabel = "TM"
    override var currentState: Int? = null

    override val transitions: List<Transition>
        get() = turingTransitions

    // Tape represented as a mutable list that can grow in both directions
    val tape: MutableList<Char> = mutableListOf()
    var headPosition: Int = 0

    override fun advanceSimulation(): Simulation {
        if (!ensureCurrentState()) {
            return Simulation.Ended(SimulationOutcome.ACTIVE)
        }
        val currentStateIndex = currentState!!

        val fromState = getStateByIndex(currentStateIndex)
        if (fromState.final) {
            return Simulation.Ended(SimulationOutcome.ACCEPTED)
        }

        val currentSymbol = readSymbol()
        val transition =
            turingTransitions.firstOrNull { transition ->
                transition.fromState == currentState &&
                    transition.read.firstOrNull() == currentSymbol
            }

        if (transition == null) {
            return Simulation.Ended(SimulationOutcome.REJECTED)
        }

        writeSymbol(transition.write)
        moveHead(transition.direction)

        val toState = getStateByIndex(transition.toState)

        return Simulation.Step(
            transitionRefs =
                listOf(
                    TransitionRef(
                        fromStateIndex = fromState.index,
                        toStateIndex = toState.index,
                        transitionIndex = turingTransitions.indexOf(transition),
                    )
                ),
            onAllComplete = {
                fromState.isCurrent = false
                toState.isCurrent = true
                currentState = toState.index
            },
        )
    }

    override fun removeTransition(transition: Transition) {
        turingTransitions.remove(transition)
    }

    override fun resetSimulation() {
        super.resetSimulation()
        tape.clear()
        if (fullInput.isNotEmpty()) {
            tape.addAll(fullInput.toList())
        } else {
            tape.add(blankSymbol)
        }
        headPosition = 0
    }

    private fun readSymbol(): Char {
        expandTapeIfNeeded()
        return tape.getOrNull(headPosition) ?: blankSymbol
    }

    private fun writeSymbol(symbol: Char) {
        expandTapeIfNeeded()
        if (headPosition in tape.indices) {
            tape[headPosition] = symbol
        }
    }

    private fun moveHead(direction: TapeDirection) {
        when (direction) {
            TapeDirection.LEFT -> {
                headPosition--
                if (headPosition < 0) {
                    tape.add(0, blankSymbol)
                    headPosition = 0
                }
            }

            TapeDirection.RIGHT -> {
                headPosition++
                expandTapeIfNeeded()
            }

            TapeDirection.STAY -> {}
        }
    }

    private fun expandTapeIfNeeded() {
        while (headPosition >= tape.size) {
            tape.add(blankSymbol)
        }
    }
}
