package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef
import com.sfag.automata.domain.tree.Branch
import com.sfag.automata.domain.tree.Tree

sealed class Machine(
    var name: String,
    val states: MutableList<State>,
    val savedInputs: MutableList<StringBuilder>,
) {
    /** JFF file format tag (e.g. "fa", "pda", "turing"). */
    abstract val jffTag: String

    /** Short type label for UI display (e.g. "FA", "PDA", "TM"). */
    abstract val typeLabel: String
    abstract val transitions: List<Transition>

    var fullInput: String = ""
        private set

    val tree = Tree()

    val initialState: State?
        get() = states.firstOrNull { it.initial }

    internal abstract fun advanceSimulation(): Simulation

    abstract fun removeTransition(transition: Transition)

    fun addNewState(state: State) {
        states.add(state)
    }

    protected abstract fun resetSimulation()

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
        tree.clear()
        initialState?.let { tree.initialize(it.name) }
        resetSimulation()
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

    /**
     * Allocates a [Branch] (with a fresh tree-node id) per new config, grouped by parent treeNodeId
     * for the frame, and pairs each branch with its new config so the per-machine `onAllComplete`
     * can copy the id into the new config.
     */
    protected fun <C : Config> buildBranches(
        currentConfigs: List<C>,
        stepResults: List<StepResult<C>>,
    ): Pair<Map<Int, List<Branch>>, List<Pair<Branch, C>>> {
        val resultsByParent = stepResults.groupBy { it.src.treeNodeId }
        val treeBranches = linkedMapOf<Int, MutableList<Branch>>()
        val pendingConfigs = mutableListOf<Pair<Branch, C>>()
        for (config in currentConfigs) {
            val results = resultsByParent[config.treeNodeId] ?: continue
            val branches = mutableListOf<Branch>()
            for (result in results) {
                val branch = tree.allocateBranch(getStateByIndex(result.transition.toState).name)
                branches.add(branch)
                pendingConfigs.add(branch to result.dest)
            }
            treeBranches[config.treeNodeId] = branches
        }
        return treeBranches to pendingConfigs
    }

    protected fun <C : Config> buildTransitionRefs(
        stepResults: List<StepResult<C>>
    ): List<TransitionRef> =
        stepResults.map { result ->
            TransitionRef(
                fromStateIndex = result.src.stateIndex,
                toStateIndex = result.transition.toState,
                transitionIndex = transitions.indexOf(result.transition),
            )
        }

    /**
     * Builds a terminal [Simulation.Ended] from the configs that satisfied the machine's accept
     * criterion. Empty `acceptingConfigs` means rejection.
     */
    protected fun terminate(acceptingConfigs: Collection<Config>): Simulation.Ended {
        if (acceptingConfigs.isEmpty()) {
            return Simulation.Ended(SimulationOutcome.REJECTED)
        }
        val acceptedIds = acceptingConfigs.mapTo(mutableSetOf()) { it.treeNodeId }
        return Simulation.Ended(
            outcome = SimulationOutcome.ACCEPTED,
            isNodeAccepting = { it in acceptedIds },
        )
    }
}
