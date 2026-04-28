package com.sfag.automata.domain.simulation

import com.sfag.automata.domain.machine.Config
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.automata.domain.tree.markSimulationEnd
import com.sfag.main.config.MAX_SIM_PRECOMPUTE_STEPS

/** Pre-computes the simulation as frames, up to `maxSteps` step frames after frame 0. */
fun Machine.precomputeFrames(maxSteps: Int = MAX_SIM_PRECOMPUTE_STEPS): List<MachineFrame> {
    resetToInitialState()

    val frames = mutableListOf<MachineFrame>()

    frames.add(
        MachineFrame(
            transitionRefs = emptyList(),
            treeBranches = emptyMap(),
            activeConfigs = snapshotConfigs(),
            outcome = SimulationOutcome.ACTIVE,
        )
    )

    var stepsTaken = 0
    while (stepsTaken < maxSteps) {
        val sim = advanceSimulation()
        when (sim) {
            is Simulation.Ended -> {
                val lastIndex = frames.lastIndex
                frames[lastIndex] =
                    frames[lastIndex].copy(
                        outcome = sim.outcome,
                        isNodeAccepting = sim.isNodeAccepting,
                    )
                break
            }

            is Simulation.Step -> {
                tree.expandActive(sim.treeBranches)
                sim.onAllComplete()
                frames.add(
                    MachineFrame(
                        transitionRefs = sim.transitionRefs,
                        treeBranches = sim.treeBranches,
                        activeConfigs = snapshotConfigs(),
                        outcome = SimulationOutcome.ACTIVE,
                    )
                )
                stepsTaken++
            }
        }
    }

    resetToInitialState()
    return frames
}

/** Rebuilds the tree to reflect simulation state at `targetIndex`. Replays from scratch. */
fun Machine.rebuildTreeForFrame(frames: List<MachineFrame>, targetIndex: Int) {
    require(targetIndex in frames.indices) { "targetIndex $targetIndex out of bounds" }

    tree.clear()
    initialState?.let { tree.initialize(it.name) }

    for (i in 1..targetIndex) {
        val frame = frames[i]
        if (frame.treeBranches.isNotEmpty()) {
            tree.expandActive(frame.treeBranches)
        }
    }

    val terminalFrame = frames[targetIndex]
    if (terminalFrame.isTerminal) {
        tree.markSimulationEnd(terminalFrame.isNodeAccepting)
    }
}

/** Indexes the machine's current configs by tree-node id for inclusion in a [MachineFrame]. */
fun Machine.snapshotConfigs(): Map<Int, Config> =
    when (this) {
        is FiniteMachine -> currentConfigs.associateBy { it.treeNodeId }
        is PushdownMachine -> currentConfigs.associateBy { it.treeNodeId }
        is TuringMachine -> currentConfigs.associateBy { it.treeNodeId }
    }
