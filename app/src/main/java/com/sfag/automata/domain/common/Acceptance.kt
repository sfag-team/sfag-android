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
    data class Config(val stateIndex: Int, val inputIndex: Int)

    val startIndex = initialState?.index ?: return false
    return bfsCheck(
        initial = Config(startIndex, 0),
        expand = { config ->
            val remaining = input.substring(minOf(config.inputIndex, input.length))
            val matching =
                if (config.inputIndex < input.length) {
                    transitions.filter {
                        it.fromState == config.stateIndex &&
                            (it.read.isEmpty() || remaining.startsWith(it.read))
                    }
                } else {
                    transitions.filter { it.fromState == config.stateIndex && it.read.isEmpty() }
                }
            matching.map { Config(it.toState, config.inputIndex + it.read.length) }
        },
        isAccepted = { config ->
            config.inputIndex == input.length && getStateByIndex(config.stateIndex).final
        },
        maxConfigs = MAX_BFS_FA_CONFIGS,
    )
}

private fun PushdownMachine.checkPdaAcceptance(input: StringBuilder): Boolean? {
    data class Config(val stateIndex: Int, val inputIndex: Int, val stack: List<Char>)

    val startIndex = initialState?.index ?: return false
    val acceptPredicate: (Config) -> Boolean =
        when (acceptanceCriteria) {
            AcceptanceCriteria.BY_FINAL_STATE -> { config ->
                    config.inputIndex == input.length && getStateByIndex(config.stateIndex).final
                }

            AcceptanceCriteria.BY_EMPTY_STACK -> { config ->
                    config.inputIndex == input.length && config.stack.isEmpty()
                }
        }
    return bfsCheck(
        initial = Config(startIndex, 0, listOf(Symbols.INITIAL_STACK_SYMBOL)),
        expand = { config ->
            val remaining = input.substring(minOf(config.inputIndex, input.length))
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
                    Config(transition.toState, config.inputIndex + transition.read.length, newStack)
                }
        },
        isAccepted = acceptPredicate,
        maxConfigs = MAX_BFS_PDA_CONFIGS,
    )
}

private fun TuringMachine.checkTmAcceptance(input: StringBuilder): Boolean? {
    data class Config(val stateIndex: Int, val tape: List<Char>, val headPosition: Int)

    val initialTape = if (input.isEmpty()) listOf(blankSymbol) else input.toList()
    val startIndex = initialState?.index ?: return false
    return bfsCheck(
        initial = Config(startIndex, initialTape, 0),
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
                    Config(transition.toState, newTape, newHead)
                }
        },
        isAccepted = { config -> getStateByIndex(config.stateIndex).final },
        maxConfigs = MAX_BFS_TM_CONFIGS,
    )
}

private fun <Config> bfsCheck(
    initial: Config,
    expand: (Config) -> List<Config>,
    isAccepted: (Config) -> Boolean,
    maxConfigs: Int,
): Boolean? {
    val visited = mutableSetOf<Config>()
    var current = mutableListOf(initial)

    while (current.isNotEmpty()) {
        val next = mutableListOf<Config>()
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
