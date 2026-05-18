package com.sfag.automata.domain.simulation

import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.MachineBlueprint
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.automata.domain.tree.Branch
import com.sfag.automata.domain.tree.markSimEnd
import com.sfag.main.config.MAX_SIM_PRECOMPUTE_FRAMES

/**
 * Pure pre-compute from an immutable [MachineBlueprint] - safe to call from background threads.
 * Builds a throwaway worker machine internally so the source blueprint is never touched.
 */
fun MachineBlueprint.precomputeFrames(
    input: String,
    maxFrames: Int = MAX_SIM_PRECOMPUTE_FRAMES,
): List<SimFrame> {
    val worker = toMachine()
    worker.setInput(input)
    return worker.precomputeFrames(maxFrames)
}

/** Pre-computes the simulation up to [maxFrames] snapshots (including the initial frame). */
fun Machine.precomputeFrames(maxFrames: Int = MAX_SIM_PRECOMPUTE_FRAMES): List<SimFrame> {
    resetToInitialState()

    val frames = mutableListOf(snapshotFrame())

    while (frames.size < maxFrames) {
        when (val advance = advanceSimulation()) {
            is SimAdvance.Ended -> {
                frames[frames.lastIndex] =
                    frames
                        .last()
                        .copy(status = advance.status, isNodeAccepting = advance.isNodeAccepting)
                break
            }

            is SimAdvance.Step -> {
                tree.expandActive(advance.treeBranches)
                advance.onAllComplete()
                frames.add(snapshotFrame(advance.transitionRefs, advance.treeBranches))
            }
        }
    }

    resetToInitialState()
    return frames
}

/** Snapshots the machine's current configs (keyed by tree node id) as a [SimFrame]. */
fun Machine.snapshotFrame(
    transitionRefs: List<TransitionRef> = emptyList(),
    treeBranches: Map<Int, List<Branch>> = emptyMap(),
): SimFrame {
    val configs: List<SimConfig> =
        when (this) {
            is FiniteMachine -> activeConfigs
            is PushdownMachine -> activeConfigs
            is TuringMachine -> activeConfigs
        }
    return SimFrame(
        transitionRefs = transitionRefs,
        treeBranches = treeBranches,
        activeConfigs = configs.associateBy { it.treeNodeId },
        status = SimStatus.ACTIVE,
    )
}

/** Rebuilds the tree to reflect simulation state at `targetIndex`. Replays from scratch. */
fun Machine.rebuildTreeForFrame(frames: List<SimFrame>, targetIndex: Int) {
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
        tree.markSimEnd(terminalFrame.isNodeAccepting)
    }
}
