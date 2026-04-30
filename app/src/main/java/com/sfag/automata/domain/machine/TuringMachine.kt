package com.sfag.automata.domain.machine

import com.sfag.automata.domain.simulation.Simulation
import com.sfag.main.config.Symbols

/**
 * A Turing machine consists of:
 * - A finite set of states Q
 * - A tape alphabet Γ (including blank symbol)
 * - An input alphabet Σ ⊆ Γ
 * - A transition function δ: Q × Γ -> Q × Γ × {L, R, S}
 * - An initial state q0
 * - A blank symbol (default '□')
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
    val currentConfigs: MutableList<Config.Tm> = mutableListOf()

    private fun initialTape(): List<Char> =
        if (fullInput.isEmpty()) listOf(blankSymbol) else fullInput.toList()

    override fun removeTransition(transition: Transition) {
        tmTransitions.remove(transition)
    }

    fun addNewTransition(
        fromState: State,
        toState: State,
        read: String,
        write: Char,
        direction: TapeDirection,
    ) {
        val alreadyExists =
            tmTransitions.any {
                it.fromState == fromState.index &&
                    it.toState == toState.index &&
                    it.read == read &&
                    it.write == write &&
                    it.direction == direction
            }
        if (!alreadyExists) {
            tmTransitions.add(TmTransition(fromState.index, toState.index, read, write, direction))
        }
    }

    override fun resetSimulation() {
        currentConfigs.clear()
        initialState?.index?.let {
            currentConfigs.add(Config.Tm(it, initialTape(), 0, treeNodeId = tree.root!!.id))
        }
    }

    override fun advanceSimulation(): Simulation {
        // TM halts on reaching a final state (JFLAP 7.1 semantics). For NTM, any active
        // config in a final state accepts overall.
        if (currentConfigs.any { getStateByIndex(it.stateIndex).final }) {
            return terminateSimulation()
        }

        val stepResults = mutableListOf<StepResult<Config.Tm>>()
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
                val newConfig = Config.Tm(transition.toState, newTape, newHead)
                stepResults.add(StepResult(src = config, dest = newConfig, transition = transition))
            }
        }
        if (stepResults.isEmpty()) {
            return terminateSimulation()
        }

        val (treeBranches, pendingConfigs) = buildBranches(currentConfigs, stepResults)

        return Simulation.Step(
            transitionRefs = buildTransitionRefs(stepResults),
            treeBranches = treeBranches,
            onAllComplete = {
                currentConfigs.clear()
                for ((branch, config) in pendingConfigs) {
                    currentConfigs.add(config.copy(treeNodeId = branch.treeNodeId))
                }
            },
        )
    }

    private fun terminateSimulation(): Simulation.Ended =
        terminate(currentConfigs.filter { getStateByIndex(it.stateIndex).final })

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
