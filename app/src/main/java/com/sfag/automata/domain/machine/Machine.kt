package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.TransitionRef
import com.sfag.automata.domain.tree.Branch
import com.sfag.automata.domain.tree.Tree

sealed class Machine(
    var name: String,
    val states: MutableList<State>,
    val savedInputs: MutableList<StringBuilder>,
    var remainingInput: StringBuilder = StringBuilder(),
) {
    abstract val transitions: List<Transition>

    /** JFF file format tag (e.g. "fa", "pda", "turing"). */
    abstract val jffTag: String

    /** Short type label for UI display (e.g. "FA", "PDA", "TM"). */
    abstract val typeLabel: String

    abstract var currentState: Int?

    val tree = Tree()

    // Snapshot of full input for simulation reset
    var fullInput: String = ""
    var currentInput = StringBuilder()

    abstract fun removeTransition(transition: Transition)

    fun removeState(state: State) {
        states.remove(state)
        transitions
            .filter { it.fromState == state.index || it.toState == state.index }
            .forEach { removeTransition(it) }
    }

    abstract fun advanceSimulation(): Simulation

    fun getStateByIndex(index: Int): State =
        states.firstOrNull { it.index == index }
            ?: throw IllegalArgumentException("State with index $index not found")

    fun getStateByIndexOrNull(index: Int): State? = states.firstOrNull { it.index == index }

    fun setInitialStateAsCurrent() {
        currentState?.let { getStateByIndexOrNull(it)?.isCurrent = false }
        tree.clear()
        states
            .firstOrNull { it.initial }
            ?.let { initialState ->
                initialState.isCurrent = true
                currentState = initialState.index
                tree.initialize(initialState.name)
            }
        resetSimulation()
    }

    open fun addNewState(state: State) {
        if (state.initial && currentState == null) {
            currentState = state.index
            state.isCurrent = true
        }
        states.add(state)
    }

    abstract fun canReachFinalState(
        input: StringBuilder,
        fromInit: Boolean,
    ): Boolean?

    /** Expands the derivation tree with exactly the transitions being processed in this step. */
    fun expandSimulationTree(
        transitionRefs: List<TransitionRef>,
        keepActive: Boolean = false,
    ) {
        val active = tree.getActiveNodes()
        if (active.isEmpty()) return

        val branches = mutableMapOf<Int, List<Branch>>()
        for (node in active) {
            val state = states.firstOrNull { it.name == node.stateName }
            if (state == null) {
                branches[node.id] = emptyList()
                continue
            }
            val toStateIndices =
                transitionRefs
                    .filter { it.fromStateIndex == state.index }
                    .map { it.toStateIndex }
                    .toMutableSet()
            if (keepActive) toStateIndices.add(state.index)
            if (toStateIndices.isEmpty()) {
                branches[node.id] = emptyList()
                continue
            }
            branches[node.id] =
                toStateIndices
                    .map { index -> Branch(getStateByIndex(index).name) }
                    .sortedBy { it.stateName }
        }
        tree.expandActive(branches)
    }

    open fun isAccepted(input: StringBuilder): Boolean? = canReachFinalState(input, true)

    open fun resetSimulation() {}

    fun findNewStateIndex(): Int {
        if (states.isEmpty()) return 1
        val usedIndices = states.map { it.index }.toSortedSet()
        for (i in 1..usedIndices.last() + 1) {
            if (i !in usedIndices) return i
        }
        return usedIndices.last() + 1
    }

    protected fun ensureCurrentState(): Boolean {
        if (currentState == null) currentState = states.firstOrNull { it.initial }?.index
        return currentState != null
    }

    protected fun findStartStateIndex(fromInit: Boolean): Int? =
        if (fromInit) {
            states.firstOrNull { it.initial }?.index
        } else {
            currentState ?: states.firstOrNull { it.initial }?.index
        }
}
