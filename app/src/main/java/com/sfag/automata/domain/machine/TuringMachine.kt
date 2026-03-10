package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef
import com.sfag.main.config.Symbols

private const val MAX_SIMULATION_STEPS = 10_000

/**
 * A Turing machine consists of:
 * - A finite set of states Q
 * - A tape alphabet Γ (including blank symbol)
 * - An input alphabet Σ ⊆ Γ
 * - A transition function δ: Q × Γ → Q × Γ × {L, R, S}
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

    override fun removeTransition(transition: Transition) {
        turingTransitions.remove(transition)
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

    override fun advanceSimulation(): Simulation {
        if (!ensureCurrentState()) return Simulation.Ended(SimulationOutcome.ACTIVE)
        val currentStateIndex = currentState!!

        val fromState = getStateByIndex(currentStateIndex)

        val currentSymbol = readSymbol()
        val transition =
            turingTransitions.firstOrNull { transition ->
                transition.fromState == currentState && transition.name.firstOrNull() == currentSymbol
            }

        if (transition == null) {
            // Halted - accept if in final state
            return Simulation.Ended(
                if (fromState.final) SimulationOutcome.ACCEPTED else SimulationOutcome.REJECTED,
            )
        }

        writeSymbol(transition.writeSymbol)
        moveHead(transition.direction)

        val toState = getStateByIndex(transition.toState)

        return Simulation.Step(
            transitionRefs =
                listOf(
                    TransitionRef(
                        fromStateIndex = fromState.index,
                        toStateIndex = toState.index,
                    ),
                ),
            onAllComplete = {
                fromState.isCurrent = false
                toState.isCurrent = true
                currentState = toState.index
            },
        )
    }

    override fun resetSimulation() {
        tape.clear()
        if (fullInput.isNotEmpty()) {
            tape.addAll(fullInput.toList())
        } else {
            tape.add(blankSymbol)
        }
        headPosition = 0
    }

    override fun canReachFinalState(
        input: StringBuilder,
        fromInit: Boolean,
    ): Boolean? {
        val maxSteps = MAX_SIMULATION_STEPS

        val simTape = input.toString().toMutableList()
        if (simTape.isEmpty()) simTape.add(blankSymbol)
        var simHead = 0
        var simState = findStartStateIndex(fromInit) ?: return false

        repeat(maxSteps) {
            val state = getStateByIndexOrNull(simState) ?: return false

            while (simHead >= simTape.size) simTape.add(blankSymbol)
            while (simHead < 0) {
                simTape.add(0, blankSymbol)
                simHead++
            }

            val symbol = simTape.getOrNull(simHead) ?: blankSymbol
            val transition =
                turingTransitions.firstOrNull { transition ->
                    transition.fromState == simState && transition.name.firstOrNull() == symbol
                } ?: return state.final

            simTape[simHead] = transition.writeSymbol
            simHead +=
                when (transition.direction) {
                    TapeDirection.LEFT -> -1
                    TapeDirection.RIGHT -> 1
                    TapeDirection.STAY -> 0
                }
            simState = transition.toState
        }

        return null
    }
}
