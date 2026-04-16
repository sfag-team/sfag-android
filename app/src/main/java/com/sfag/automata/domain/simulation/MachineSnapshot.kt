package com.sfag.automata.domain.simulation

import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TuringMachine

fun Machine.snapshotActiveNodes(): Map<Int, NodeSnapshot> {
    val result = mutableMapOf<Int, NodeSnapshot>()
    when (this) {
        is FiniteMachine -> {
            val consumed = fullInput.length - remainingInput.length
            for (config in currentConfigs) {
                if (config.treeNodeId >= 0) {
                    result[config.treeNodeId] =
                        NodeSnapshot.FaNodeSnapshot(inputConsumed = consumed + config.inputOffset)
                }
            }
        }

        is PushdownMachine -> {
            val consumed = fullInput.length - remainingInput.length
            for (config in currentConfigs) {
                if (config.treeNodeId >= 0) {
                    result[config.treeNodeId] =
                        NodeSnapshot.PdaNodeSnapshot(
                            inputConsumed = consumed + config.inputOffset,
                            stack = config.stack.toList(),
                        )
                }
            }
        }

        is TuringMachine ->
            for (config in currentConfigs) {
                if (config.treeNodeId >= 0) {
                    result[config.treeNodeId] =
                        NodeSnapshot.TmNodeSnapshot(
                            tape = config.tape,
                            headPosition = config.headPosition,
                        )
                }
            }
    }
    return result
}
