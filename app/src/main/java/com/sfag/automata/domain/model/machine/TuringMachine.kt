package com.sfag.automata.domain.model.machine

import com.sfag.automata.domain.model.simulation.SimulationResult
import com.sfag.automata.domain.model.simulation.TransitionData
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.TapeDirection
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.automata.domain.model.transition.TuringTransition
import com.sfag.shared.util.Symbols
import com.sfag.shared.util.XmlUtils.escapeXml
import com.sfag.shared.util.XmlUtils.xmlTag

/**
 * Turing Machine implementation.
 *
 * A Turing machine consists of:
 * - A finite set of states Q
 * - A tape alphabet Γ (including blank symbol)
 * - An input alphabet Σ ⊆ Γ
 * - A transition function δ: Q × Γ → Q × Γ × {L, R, S}
 * - An initial state q0
 * - A blank symbol (default 'B')
 * - A set of accepting/final states F
 */
@Suppress("UNCHECKED_CAST")
class TuringMachine(
    name: String = "",
    version: Int = 1,
    states: MutableList<State> = mutableListOf(),
    transitions: MutableList<TuringTransition> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    val blankSymbol: Char = Symbols.BLANK
) : Machine(
    name, version,
    machineType = MachineType.Turing,
    states, transitions as MutableList<Transition>,
    savedInputs = savedInputs
) {
    override var currentState: Int? = null

    // Tape represented as a mutable list that can grow in both directions
    val tape: MutableList<Char> = mutableListOf()
    var headPosition: Int = 0

    private fun readSymbol(): Char {
        expandTapeIfNeeded()
        return tape.getOrNull(headPosition) ?: blankSymbol
    }

    private fun writeSymbol(symbol: Char) {
        expandTapeIfNeeded()
        if (headPosition >= 0 && headPosition < tape.size) {
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
            TapeDirection.STAY -> { /* Do nothing */ }
        }
    }

    private fun expandTapeIfNeeded() {
        while (headPosition >= tape.size) {
            tape.add(blankSymbol)
        }
        while (headPosition < 0) {
            tape.add(0, blankSymbol)
            headPosition++
        }
    }

    override fun calculateNextStep(): SimulationResult {
        if (!ensureCurrentState()) return SimulationResult.Ended(null)
        val currentStateIndex = currentState!!

        val startState = getStateByIndex(currentStateIndex)

        // Check if we're in a final state
        if (startState.final) {
            return SimulationResult.Ended(true)
        }

        val currentSymbol = readSymbol()
        val possibleTransitions = transitions.filterIsInstance<TuringTransition>().filter { t ->
            t.startState == currentState && t.name.firstOrNull() == currentSymbol
        }

        if (possibleTransitions.isEmpty()) {
            // Halted - no transition available
            return SimulationResult.Ended(false)
        }

        // Turing machines are typically deterministic
        val transition = possibleTransitions.first()

        // Execute transition (write and move are done before animation)
        writeSymbol(transition.writeSymbol)
        moveHead(transition.direction)

        val endState = getStateByIndex(transition.endState)

        return SimulationResult.Step(
            transitions = listOf(TransitionData(startStateIndex = startState.index, endStateIndex = endState.index)),
            onAllComplete = {
                startState.isCurrent = false
                endState.isCurrent = true
                currentState = endState.index
            }
        )
    }

    /**
     * Checks if input was fully consumed and machine is in accepting state.
     */
    fun isAccepted(): Boolean? {
        val currentIdx = currentState ?: return null
        val state = getStateByIndexOrNull(currentIdx) ?: return null
        return if (state.final) true else null
    }


    override fun addNewState(state: State) {
        if (state.initial && currentState == null) {
            currentState = state.index
            state.isCurrent = true
        }
        states.add(state)
    }

    override fun resetMachineState() {
        tape.clear()
        if (fullInputSnapshot.isNotEmpty()) {
            tape.addAll(fullInputSnapshot.toList())
        } else {
            tape.add(blankSymbol)
        }
        headPosition = 0
    }

    override fun expandDerivationTree() {
        // Turing machines: no derivation tree
    }

    override fun getMathFormatData(): MachineFormatData {
        val turingTransitions = transitions.filterIsInstance<TuringTransition>()

        val transitionDescriptions = turingTransitions.map { t ->
            val fromState = getStateByIndexOrNull(t.startState)?.name ?: "?"
            val toState = getStateByIndexOrNull(t.endState)?.name ?: "?"
            val readSymbol = t.name.ifEmpty { Symbols.EPSILON }
            "${Symbols.DELTA}($fromState, $readSymbol) = ($toState, ${t.writeSymbol}, ${t.direction})"
        }

        val tapeAlphabetSet = turingTransitions
            .flatMap { listOf(it.name.firstOrNull(), it.writeSymbol) }
            .filterNotNull()
            .toSet()
            .plus(blankSymbol)

        return MachineFormatData(
            stateNames = states.map { it.name },
            inputAlphabet = transitions.mapNotNull { it.name.firstOrNull() }.toSet(),
            initialStateName = states.firstOrNull { it.initial }?.name ?: "q0",
            finalStateNames = states.filter { it.final }.map { it.name },
            transitionDescriptions = transitionDescriptions,
            machineType = machineType,
            tapeAlphabet = tapeAlphabetSet,
            blankSymbol = blankSymbol
        )
    }

    override fun canReachFinalState(input: StringBuilder, fromInit: Boolean): Boolean {
        // For Turing machines, this is undecidable in general
        // We do bounded simulation
        val maxSteps = 1000
        var steps = 0

        val tempTape = input.toString().toMutableList()
        if (tempTape.isEmpty()) tempTape.add(blankSymbol)
        var tempHead = 0
        var tempState = if (fromInit) {
            states.firstOrNull { it.initial }?.index
        } else {
            currentState
        }

        while (steps < maxSteps && tempState != null) {
            val state = states.firstOrNull { it.index == tempState } ?: break
            if (state.final) return true

            while (tempHead >= tempTape.size) tempTape.add(blankSymbol)
            while (tempHead < 0) {
                tempTape.add(0, blankSymbol)
                tempHead++
            }

            val symbol = tempTape.getOrNull(tempHead) ?: blankSymbol
            val transition = transitions.filterIsInstance<TuringTransition>().firstOrNull { t ->
                t.startState == tempState && t.name.firstOrNull() == symbol
            } ?: break

            tempTape[tempHead] = transition.writeSymbol
            tempHead += when (transition.direction) {
                TapeDirection.LEFT -> -1
                TapeDirection.RIGHT -> 1
                TapeDirection.STAY -> 0
            }
            tempState = transition.endState
            steps++
        }

        return false
    }

    override fun exportTransitionsToJFF(builder: StringBuilder) {
        for (transition in transitions.filterIsInstance<TuringTransition>()) {
            builder.appendLine("        <transition>")
            builder.appendLine("            <from>${transition.startState}</from>")
            builder.appendLine("            <to>${transition.endState}</to>")
            builder.appendLine("            ${xmlTag("read", transition.name)}")
            builder.appendLine("            <write>${escapeXml(transition.writeSymbol.toString())}</write>")
            builder.appendLine("            <move>${transition.direction.symbol}</move>")
            builder.appendLine("        </transition>")
        }
    }
}
