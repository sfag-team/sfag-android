package com.sfag.automata.domain.simulation

import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TuringMachine

fun Machine.snapshotActiveNodes(): Map<Int, NodeSnapshot> {
    val active = tree.getActiveNodes()
    when (this) {
        is FiniteMachine -> {
            val remaining = currentConfigs.toMutableList()
            val consumed = fullInput.length - remainingInput.length
            val result = mutableMapOf<Int, NodeSnapshot>()
            for (node in active) {
                val state = states.firstOrNull { it.name == node.stateName } ?: continue
                val matchIndex = remaining.indexOfFirst { it.stateIndex == state.index }
                if (matchIndex >= 0) {
                    val config = remaining.removeAt(matchIndex)
                    result[node.id] =
                        NodeSnapshot.FaSnapshot(inputConsumed = consumed + config.inputOffset)
                } else {
                    result[node.id] = NodeSnapshot.FaSnapshot(inputConsumed = consumed)
                }
            }
            return result
        }

        is PushdownMachine -> {
            val remaining = currentConfigs.toMutableList()
            val consumed = fullInput.length - remainingInput.length
            val result = mutableMapOf<Int, NodeSnapshot>()
            for (node in active) {
                val state = states.firstOrNull { it.name == node.stateName } ?: continue
                val matchIndex = remaining.indexOfFirst { it.stateIndex == state.index }
                if (matchIndex >= 0) {
                    val config = remaining.removeAt(matchIndex)
                    result[node.id] =
                        NodeSnapshot.PdaSnapshot(
                            inputConsumed = consumed + config.inputOffset,
                            stack = config.stack.toList(),
                        )
                } else {
                    result[node.id] =
                        NodeSnapshot.PdaSnapshot(
                            inputConsumed = consumed,
                            stack = activeNodeStacks[node.id] ?: listOf('Z'),
                        )
                }
            }
            return result
        }

        is TuringMachine -> {
            val result = mutableMapOf<Int, NodeSnapshot>()
            for (node in active) {
                result[node.id] =
                    NodeSnapshot.TmSnapshot(tape = tape.toList(), headPosition = headPosition)
            }
            return result
        }
    }
}
