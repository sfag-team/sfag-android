package com.sfag.automata.domain.common

import com.sfag.automata.domain.machine.AcceptanceCriteria
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TapeDirection
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.automata.domain.machine.applyStackOp
import com.sfag.automata.domain.machine.stackTopMatches
import com.sfag.main.config.MAX_BFS_FA_CONFIGS
import com.sfag.main.config.MAX_BFS_PDA_CONFIGS
import com.sfag.main.config.MAX_BFS_TM_CONFIGS
import com.sfag.main.config.Symbols

/**
 * BFS-based acceptance check for the input editor. Tests whether the machine accepts the given
 * input string without running the simulation. Returns true (accepted), false (rejected), or null
 * (inconclusive - config limit reached).
 */
fun Machine.checkAcceptance(input: StringBuilder): Boolean? =
    when (this) {
        is FiniteMachine -> checkFaAcceptance(input)
        is PushdownMachine -> checkPdaAcceptance(input)
        is TuringMachine -> checkTmAcceptance(input)
    }

private fun FiniteMachine.checkFaAcceptance(input: StringBuilder): Boolean? {
    data class BfsConfig(val stateIndex: Int, val inputConsumed: Int)

    val startIndex = initialState?.index ?: return false
    return bfsCheck(
        initial = BfsConfig(startIndex, 0),
        expand = { config ->
            val remaining = input.substring(minOf(config.inputConsumed, input.length))
            val matching =
                if (config.inputConsumed < input.length) {
                    transitions.filter {
                        it.fromState == config.stateIndex &&
                            (it.read.isEmpty() || remaining.startsWith(it.read))
                    }
                } else {
                    transitions.filter { it.fromState == config.stateIndex && it.read.isEmpty() }
                }
            matching.map { BfsConfig(it.toState, config.inputConsumed + it.read.length) }
        },
        isAccepted = { config ->
            config.inputConsumed == input.length && getStateByIndex(config.stateIndex).final
        },
        maxConfigs = MAX_BFS_FA_CONFIGS,
    )
}

private fun PushdownMachine.checkPdaAcceptance(input: StringBuilder): Boolean? {
    data class BfsConfig(val stateIndex: Int, val inputConsumed: Int, val stack: List<Char>)

    val startIndex = initialState?.index ?: return false
    val acceptPredicate: (BfsConfig) -> Boolean =
        when (acceptanceCriteria) {
            AcceptanceCriteria.BY_FINAL_STATE -> { config ->
                    config.inputConsumed == input.length && getStateByIndex(config.stateIndex).final
                }

            AcceptanceCriteria.BY_EMPTY_STACK -> { config ->
                    config.inputConsumed == input.length && config.stack.isEmpty()
                }
        }
    return bfsCheck(
        initial = BfsConfig(startIndex, 0, listOf(Symbols.INITIAL_STACK_SYMBOL)),
        expand = { config ->
            val remaining = input.substring(minOf(config.inputConsumed, input.length))
            pdaTransitions
                .filter {
                    it.fromState == config.stateIndex &&
                        (it.read.isEmpty() || remaining.startsWith(it.read)) &&
                        (it.pop.isEmpty() || stackTopMatches(config.stack, it.pop))
                }
                .mapNotNull { transition ->
                    val newStack =
                        applyStackOp(config.stack, transition.pop, transition.push)
                            ?: return@mapNotNull null
                    BfsConfig(
                        transition.toState,
                        config.inputConsumed + transition.read.length,
                        newStack,
                    )
                }
        },
        isAccepted = acceptPredicate,
        maxConfigs = MAX_BFS_PDA_CONFIGS,
    )
}

private fun TuringMachine.checkTmAcceptance(input: StringBuilder): Boolean? {
    data class BfsConfig(val stateIndex: Int, val tape: List<Char>, val headPosition: Int)

    val initialTape = if (input.isEmpty()) listOf(blankSymbol) else input.toList()
    val startIndex = initialState?.index ?: return false
    return bfsCheck(
        initial = BfsConfig(startIndex, initialTape, 0),
        expand = { config ->
            val tape = config.tape.toMutableList()
            val head = config.headPosition
            while (head >= tape.size) {
                tape.add(blankSymbol)
            }
            val symbol = tape.getOrNull(head) ?: blankSymbol
            tmTransitions
                .filter { it.fromState == config.stateIndex && it.read.firstOrNull() == symbol }
                .map { transition ->
                    val newTape = tape.toMutableList()
                    newTape[head] = transition.write
                    var newHead =
                        head +
                            when (transition.direction) {
                                TapeDirection.LEFT -> -1
                                TapeDirection.RIGHT -> 1
                                TapeDirection.STAY -> 0
                            }
                    if (newHead < 0) {
                        newTape.add(0, blankSymbol)
                        newHead = 0
                    }
                    BfsConfig(transition.toState, newTape, newHead)
                }
        },
        isAccepted = { config -> getStateByIndex(config.stateIndex).final },
        maxConfigs = MAX_BFS_TM_CONFIGS,
    )
}

private fun <T> bfsCheck(
    initial: T,
    expand: (T) -> List<T>,
    isAccepted: (T) -> Boolean,
    maxConfigs: Int,
): Boolean? {
    val visited = mutableSetOf<T>()
    var current = mutableListOf(initial)

    while (current.isNotEmpty()) {
        val next = mutableListOf<T>()
        for (config in current) {
            if (visited.size >= maxConfigs) {
                return null
            }
            if (!visited.add(config)) {
                continue
            }
            if (isAccepted(config)) {
                return true
            }
            next.addAll(expand(config))
        }
        current = next
    }

    return false
}
