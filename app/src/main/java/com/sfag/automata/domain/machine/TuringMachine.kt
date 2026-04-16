package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef
import com.sfag.automata.domain.tree.Branch
import com.sfag.main.config.Symbols

data class TmConfig(
    val stateIndex: Int,
    val tape: List<Char>,
    val headPosition: Int,
    val treeNodeId: Int = -1,
)

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
    val tmTransitions: MutableList<TmTransition> = mutableListOf(),
    val blankSymbol: Char = Symbols.BLANK_CHAR,
) : Machine(name, states, savedInputs = savedInputs) {
    override val jffTag = "turing"
    override val typeLabel = "TM"

    override val transitions: List<Transition>
        get() = tmTransitions

    // Track multiple current configs for NTM simulation
    val currentConfigs: MutableList<TmConfig> = mutableListOf()

    override var currentState: Int?
        get() = currentConfigs.firstOrNull()?.stateIndex
        set(value) {
            currentConfigs.clear()
            value?.let { currentConfigs.add(TmConfig(it, initialTape(), 0)) }
        }

    /** First config's tape — UI accessor for single-path display. */
    val tape: List<Char>
        get() = currentConfigs.firstOrNull()?.tape ?: listOf(blankSymbol)

    /** First config's head position — UI accessor for single-path display. */
    val headPosition: Int
        get() = currentConfigs.firstOrNull()?.headPosition ?: 0

    private fun initialTape(): List<Char> =
        if (fullInput.isEmpty()) listOf(blankSymbol) else fullInput.toList()

    override fun removeTransition(transition: Transition) {
        tmTransitions.remove(transition)
    }

    override fun resetSimulation() {
        super.resetSimulation()
        currentConfigs.clear()
        initialState?.index?.let {
            currentConfigs.add(TmConfig(it, initialTape(), 0, treeNodeId = tree.root!!.id))
        }
    }

    override fun calculateAllPathsStep(): Simulation {
        // TM halts on reaching a final state (JFLAP 7.1 semantics). For NTM, any active
        // config in a final state accepts overall.
        if (currentConfigs.any { getStateByIndex(it.stateIndex).final }) {
            return terminateSimulation()
        }

        val allResults = mutableListOf<Triple<TmConfig, TmTransition, TmConfig>>()

        for (config in currentConfigs) {
            val symbol = config.tape.getOrElse(config.headPosition) { blankSymbol }
            for (transition in tmTransitions) {
                if (
                    transition.fromState != config.stateIndex ||
                        transition.read.firstOrNull() != symbol
                ) {
                    continue
                }
                val (newTape, newHead) = applyTmStep(config.tape, config.headPosition, transition)
                allResults.add(
                    Triple(config, transition, TmConfig(transition.toState, newTape, newHead))
                )
            }
        }

        if (allResults.isEmpty()) {
            return terminateSimulation()
        }

        // Build treeBranches and pair each new config with its branch (the branch receives
        // its tree node id when the tree expands, so configs can read it back directly)
        val resultsByParent = allResults.groupBy { it.first.treeNodeId }
        val treeBranches = linkedMapOf<Int, MutableList<Branch>>()
        val pendingConfigs = mutableListOf<Pair<Branch, TmConfig>>()

        for (config in currentConfigs) {
            val results = resultsByParent[config.treeNodeId] ?: continue
            val branches = mutableListOf<Branch>()
            for ((_, transition, newConfig) in results) {
                val toState = getStateByIndex(transition.toState)
                val branch = Branch(toState.name)
                branches.add(branch)
                pendingConfigs.add(branch to newConfig)
            }
            treeBranches[config.treeNodeId] = branches
        }

        val transitionRefs =
            allResults.map { (config, transition, _) ->
                TransitionRef(
                    fromStateIndex = config.stateIndex,
                    toStateIndex = transition.toState,
                    transitionIndex = tmTransitions.indexOf(transition),
                )
            }

        return Simulation.Step(
            transitionRefs = transitionRefs,
            treeBranches = treeBranches,
            onAllComplete = {
                for (config in currentConfigs) {
                    getStateByIndex(config.stateIndex).isCurrent = false
                }
                currentConfigs.clear()
                for ((branch, config) in pendingConfigs) {
                    currentConfigs.add(config.copy(treeNodeId = branch.assignedId))
                }
                for (config in currentConfigs) {
                    getStateByIndex(config.stateIndex).isCurrent = true
                }
            },
        )
    }

    private fun terminateSimulation(): Simulation.Ended {
        val acceptingConfigs = currentConfigs.filter { getStateByIndex(it.stateIndex).final }
        val anyAccepting = acceptingConfigs.isNotEmpty()
        return Simulation.Ended(
            outcome = if (anyAccepting) SimulationOutcome.ACCEPTED else SimulationOutcome.REJECTED,
            isNodeAccepting =
                if (anyAccepting) { node -> acceptingConfigs.any { it.treeNodeId == node.id } }
                else {
                    null
                },
        )
    }

    private fun applyTmStep(
        tape: List<Char>,
        headPosition: Int,
        transition: TmTransition,
    ): Pair<List<Char>, Int> {
        val newTape = tape.toMutableList()
        while (headPosition >= newTape.size) {
            newTape.add(blankSymbol)
        }
        newTape[headPosition] = transition.write
        var newHead =
            headPosition +
                when (transition.direction) {
                    TapeDirection.LEFT -> -1
                    TapeDirection.RIGHT -> 1
                    TapeDirection.STAY -> 0
                }
        if (newHead < 0) {
            newTape.add(0, blankSymbol)
            newHead = 0
        } else {
            while (newHead >= newTape.size) {
                newTape.add(blankSymbol)
            }
        }
        return newTape to newHead
    }
}
