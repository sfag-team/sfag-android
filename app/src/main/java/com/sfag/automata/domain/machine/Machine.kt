package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.snapshotActiveNodes
import com.sfag.automata.domain.tree.Tree

sealed class Machine(
    var name: String,
    val states: MutableList<State>,
    val savedInputs: MutableList<StringBuilder>,
    var remainingInput: StringBuilder = StringBuilder(),
) {
    /** JFF file format tag (e.g. "fa", "pda", "turing"). */
    abstract val jffTag: String

    /** Short type label for UI display (e.g. "FA", "PDA", "TM"). */
    abstract val typeLabel: String
    abstract var currentState: Int?
    abstract val transitions: List<Transition>

    var fullInput: String = ""
        private set

    val tree = Tree()

    val initialState: State?
        get() = states.firstOrNull { it.initial }

    fun advanceSimulation(): Simulation {
        if (currentState == null) {
            if (!ensureCurrentState()) {
                return Simulation.Ended(SimulationOutcome.ACTIVE)
            }
            getStateByIndex(currentState!!).isCurrent = true
        }
        return calculateAllPathsStep()
    }

    protected abstract fun calculateAllPathsStep(): Simulation

    abstract fun removeTransition(transition: Transition)

    open fun addNewState(state: State) {
        if (state.initial && currentState == null) {
            currentState = state.index
            state.isCurrent = true
        }
        states.add(state)
    }

    open fun resetSimulation() {
        val initialStateIndex = initialState?.index
        for (state in states) {
            state.isCurrent = state.index == initialStateIndex
        }
    }

    fun setInput(input: String) {
        fullInput = input
        resetToInitialState()
    }

    fun removeState(state: State) {
        states.remove(state)
        transitions
            .filter { it.fromState == state.index || it.toState == state.index }
            .forEach { removeTransition(it) }
    }

    fun getStateByIndex(index: Int): State =
        states.firstOrNull { it.index == index }
            ?: throw IllegalArgumentException("State with index $index not found")

    fun getStateByIndexOrNull(index: Int): State? = states.firstOrNull { it.index == index }

    fun resetToInitialState() {
        currentState?.let { getStateByIndexOrNull(it)?.isCurrent = false }
        tree.clear()
        initialState?.let { state ->
            state.isCurrent = true
            currentState = state.index
            tree.initialize(state.name)
        }
        remainingInput = StringBuilder(fullInput)
        resetSimulation()
        tree.attachSnapshots(snapshotActiveNodes())
    }

    fun findNewStateIndex(): Int {
        if (states.isEmpty()) {
            return 1
        }
        val usedIndices = states.map { it.index }.toSortedSet()
        for (i in 1..usedIndices.last() + 1) {
            if (i !in usedIndices) {
                return i
            }
        }
        return usedIndices.last() + 1
    }

    protected fun consumeInput(length: Int) {
        if (length > 0 && remainingInput.isNotEmpty()) {
            remainingInput.delete(0, minOf(length, remainingInput.length))
        }
    }

    protected fun ensureCurrentState(): Boolean {
        if (currentState == null && initialState != null) {
            resetToInitialState()
        }
        return currentState != null
    }
}
