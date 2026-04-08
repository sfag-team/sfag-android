package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
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

    abstract fun advanceSimulation(): Simulation

    abstract fun canReachFinalState(input: StringBuilder, fromInit: Boolean): Boolean?

    abstract fun removeTransition(transition: Transition)

    open fun addNewState(state: State) {
        if (state.initial && currentState == null) {
            currentState = state.index
            state.isCurrent = true
        }
        states.add(state)
    }

    open fun isAccepted(input: StringBuilder): Boolean? = canReachFinalState(input, true)

    open fun resetSimulation() {
        val initialStateIndex = states.firstOrNull { it.initial }?.index
        for (state in states) {
            state.isCurrent = state.index == initialStateIndex
        }
    }

    fun loadInput(input: String) {
        fullInput = input
        setInitialStateAsCurrent()
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
        if (currentState == null) {
            currentState = states.firstOrNull { it.initial }?.index
        }
        return currentState != null
    }

    protected fun findStartStateIndex(fromInit: Boolean): Int? =
        if (fromInit) {
            states.firstOrNull { it.initial }?.index
        } else {
            currentState ?: states.firstOrNull { it.initial }?.index
        }

    /**
     * Generic BFS reachability check. Explores configurations breadth-first using a visited set
     * (configs must implement structural equality via data class).
     *
     * @return true = accepted, false = rejected (exhausted), null = inconclusive (limit hit)
     */
    protected fun <Config> bfsReachability(
        initialConfigs: List<Config>,
        expand: (Config) -> List<Config>,
        isAccepted: (Config) -> Boolean,
        maxConfigs: Int,
    ): Boolean? {
        val visited = mutableSetOf<Config>()
        var current = initialConfigs.toMutableList()

        while (current.isNotEmpty() && visited.size < maxConfigs) {
            val next = mutableListOf<Config>()
            for (config in current) {
                if (!visited.add(config)) {
                    continue
                }
                if (visited.size > maxConfigs) {
                    break
                }
                if (isAccepted(config)) {
                    return true
                }
                next.addAll(expand(config))
            }
            current = next
        }

        return if (current.isEmpty()) false else null
    }
}
