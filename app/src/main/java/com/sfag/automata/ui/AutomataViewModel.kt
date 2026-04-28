package com.sfag.automata.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sfag.automata.data.AutomataStorage
import com.sfag.automata.data.exportToJff
import com.sfag.automata.domain.machine.Config
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.simulation.MachineFrame
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.precomputeFrames
import com.sfag.automata.domain.simulation.rebuildTreeForFrame
import com.sfag.automata.domain.simulation.snapshotConfigs
import com.sfag.main.config.INITIAL_ZOOM
import com.sfag.main.config.MAX_SIM_PRECOMPUTE_STEPS
import com.sfag.main.data.Point2D
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// JFLAP coordinate conversion (96 DPI desktop to 160 DPI Android dp) - protocol constant
private const val JFLAP_TO_DP = 160f / 96f

@HiltViewModel
class AutomataViewModel @Inject internal constructor(private val storage: AutomataStorage) :
    ViewModel() {
    // The single current machine - observed by the Activity to key the composition.
    var currentMachine by mutableStateOf<Machine?>(null)
        private set

    // Layout state - persists across simulation/edit mode switches
    val statePositions: SnapshotStateMap<Int, Offset> = mutableStateMapOf()
    var offsetXCanvas by mutableFloatStateOf(0f)
    var offsetYCanvas by mutableFloatStateOf(0f)
    var scaleCanvas by mutableFloatStateOf(1f)
    var machineAutoCenter by mutableStateOf(false)

    // Pending example to load after user confirms replacing saved machine
    var pendingExampleUri by mutableStateOf<String?>(null)
    var pendingExampleName by mutableStateOf<String?>(null)

    // Historical node inspection
    private var inspectedNodeId by mutableStateOf<Int?>(null)

    // Pre-computed simulation frames. Null until the user starts stepping.
    private var frames by mutableStateOf<List<MachineFrame>?>(null)

    private var currentFrameIndex by mutableIntStateOf(0)

    // Default branch (tree node ids by depth) Tape/Stack/Tree follow when nothing is inspected.
    // Accepting branch wins; else longest. Ties broken alphabetically per branching point.
    private var selectedPath by mutableStateOf<List<Int>>(emptyList())

    /** Frame the UI renders. Synthesized from the machine when frames aren't computed yet. */
    val currentFrame: MachineFrame?
        get() =
            frames?.getOrNull(currentFrameIndex)
                ?: currentMachine?.let { machine ->
                    MachineFrame(
                        transitionRefs = emptyList(),
                        treeBranches = emptyMap(),
                        activeConfigs = machine.snapshotConfigs(),
                        outcome = SimulationOutcome.ACTIVE,
                    )
                }

    /** Tree node id Tape/Stack/Tree render: inspected wins, else selected branch, else first. */
    val displayNodeId: Int?
        get() =
            inspectedNodeId
                ?: selectedPath.getOrNull(currentFrameIndex)
                ?: currentFrame?.activeConfigs?.keys?.firstOrNull()

    /** Config for [displayNodeId], looking back to the last frame the node was active in. */
    val displayConfig: Config?
        get() {
            val nodeId = displayNodeId ?: return null
            val list = frames ?: return currentFrame?.activeConfigs?.get(nodeId)
            for (i in currentFrameIndex downTo 0) {
                list[i].activeConfigs[nodeId]?.let {
                    return it
                }
            }
            return null
        }

    // Track unsaved changes
    var hasUnsavedChanges by mutableStateOf(false)
        private set

    // Single-threaded dispatcher serializes saves in submission order (latest state wins)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val saveDispatcher = Dispatchers.IO.limitedParallelism(1)

    fun inspectNode(nodeId: Int) {
        inspectedNodeId = nodeId
    }

    fun clearInspection() {
        inspectedNodeId = null
    }

    /** Pre-computes frames on first access. */
    private fun ensureFrames(machine: Machine): List<MachineFrame> {
        val cached = frames
        if (cached != null) {
            return cached
        }
        val computed = machine.precomputeFrames()
        selectedPath = computeSelectedPath(computed)
        frames = computed
        return computed
    }

    /** Extends frames by another batch when stepping past the end with sim still ACTIVE. */
    private fun extendFrames(machine: Machine) {
        val list = frames ?: return
        if (list.last().outcome != SimulationOutcome.ACTIVE) {
            return
        }
        val extended = machine.precomputeFrames((list.size - 1) + MAX_SIM_PRECOMPUTE_STEPS)
        selectedPath = computeSelectedPath(extended)
        frames = extended
        machine.rebuildTreeForFrame(extended, currentFrameIndex)
    }

    private fun computeSelectedPath(frames: List<MachineFrame>): List<Int> {
        if (frames.isEmpty()) {
            return emptyList()
        }
        val rootId = frames[0].activeConfigs.keys.firstOrNull() ?: return emptyList()

        // Build tree topology and per-node depth from frame data.
        val children = mutableMapOf<Int, List<Pair<Int, String>>>()
        val depthOf = mutableMapOf(rootId to 0)
        for (frame in frames) {
            for ((parentId, branches) in frame.treeBranches) {
                children[parentId] = branches.map { it.treeNodeId to it.stateName }
                val parentDepth = depthOf[parentId] ?: continue
                for (branch in branches) {
                    depthOf[branch.treeNodeId] = parentDepth + 1
                }
            }
        }
        val leafIds = depthOf.keys.filter { children[it].isNullOrEmpty() }

        // Candidates: accepting leaves if the sim accepted, else deepest leaves.
        val isAccepting = frames.last().isNodeAccepting
        val candidates: Set<Int> =
            if (isAccepting != null) {
                leafIds.filter(isAccepting).toSet()
            } else {
                val maxDepth = leafIds.maxOfOrNull { depthOf[it] ?: 0 } ?: return emptyList()
                leafIds.filter { depthOf[it] == maxDepth }.toSet()
            }
        if (candidates.isEmpty()) {
            return emptyList()
        }

        // Memoized "subtree contains a candidate" check.
        val containsCandidate = mutableMapOf<Int, Boolean>()
        fun has(id: Int): Boolean {
            containsCandidate[id]?.let {
                return it
            }
            val result = id in candidates || (children[id]?.any { has(it.first) } == true)
            containsCandidate[id] = result
            return result
        }

        // Greedy walk from root, picking the alphabetically smallest viable child.
        val path = mutableListOf<Int>()
        var current = rootId
        while (true) {
            path.add(current)
            if (current in candidates) {
                break
            }
            val viable = children[current]?.filter { has(it.first) } ?: emptyList()
            val next = viable.minByOrNull { it.second } ?: break
            current = next.first
        }
        return path
    }

    /** Returns the frame after the current one, pre-computing or extending as needed. */
    fun peekNextFrame(machine: Machine): MachineFrame? {
        var list = ensureFrames(machine)
        val nextIndex = currentFrameIndex + 1
        if (nextIndex >= list.size && list.last().outcome == SimulationOutcome.ACTIVE) {
            extendFrames(machine)
            list = frames ?: list
        }
        return list.getOrNull(nextIndex)
    }

    /** Atomically advances/rewinds playback: updates index, rebuilds tree, re-syncs inspection. */
    fun setFrameIndex(machine: Machine, newIndex: Int) {
        val list = ensureFrames(machine)
        require(newIndex in list.indices) { "frame index $newIndex out of bounds" }
        currentFrameIndex = newIndex
        machine.rebuildTreeForFrame(list, newIndex)
        inspectedNodeId?.let { nodeId ->
            if (machine.tree.findNode(nodeId) == null) {
                clearInspection()
            }
        }
    }

    fun stepForward(machine: Machine) {
        if (peekNextFrame(machine) != null) {
            setFrameIndex(machine, currentFrameIndex + 1)
        }
    }

    /** Resets playback. Call when machine structure or input changes. */
    fun invalidateSimulation(machine: Machine) {
        clearSimulationState()
        machine.resetToInitialState()
    }

    private fun clearSimulationState() {
        frames = null
        currentFrameIndex = 0
        selectedPath = emptyList()
        clearInspection()
    }

    fun markDirty() {
        hasUnsavedChanges = true
    }

    fun markSaved() {
        hasUnsavedChanges = false
    }

    /**
     * Sets the active machine and resets view to default scale + center. JFLAP coordinates (96 DPI)
     * are scaled to dp (160 DPI) via JFLAP_TO_DP.
     */
    fun setCurrentMachine(machine: Machine, positions: Map<Int, Point2D> = emptyMap()) {
        clearSimulationState()
        currentMachine = machine
        machine.resetToInitialState()
        loadPositions(positions)
        scaleCanvas = INITIAL_ZOOM
        machineAutoCenter = true
        markSaved()
    }

    /** Persists the current machine to the fixed auto-save slot. */
    fun autoSave(machine: Machine) {
        // Capture all mutable state on the main thread to avoid concurrent reads
        val jffContent = machine.exportToJff(getPositions())
        val metadata =
            storage.buildMetadata(
                machine,
                offsetXCanvas,
                offsetYCanvas,
                scaleCanvas,
                hasUnsavedChanges,
            )
        viewModelScope.launch(saveDispatcher) { storage.save(jffContent, metadata) }
    }

    /** Loads the auto-saved machine. Returns true on success. */
    fun loadMachine(): Boolean {
        val stored = storage.load() ?: return false
        clearSimulationState()
        currentMachine = stored.machine
        stored.machine.resetToInitialState()
        loadPositions(stored.positions)
        offsetXCanvas = stored.offsetX
        offsetYCanvas = stored.offsetY
        scaleCanvas = stored.scale
        if (stored.dirty) {
            markDirty()
        } else {
            markSaved()
        }
        return true
    }

    /** Returns current positions as JFLAP units for JFF export/save (dp -> JFLAP). */
    fun getPositions(): Map<Int, Point2D> =
        statePositions.mapValues { (_, offset) ->
            Point2D(offset.x / JFLAP_TO_DP, offset.y / JFLAP_TO_DP)
        }

    /** Updates a single state's position (called on drag). */
    fun updateStatePosition(stateIndex: Int, delta: Offset) {
        statePositions[stateIndex]?.let { statePositions[stateIndex] = it + delta }
    }

    /** Assigns a position to a newly created state. */
    fun addStatePosition(stateIndex: Int, offset: Offset) {
        statePositions[stateIndex] = offset
    }

    /** Converts JFLAP positions to dp and loads them into statePositions. */
    private fun loadPositions(positions: Map<Int, Point2D>) {
        statePositions.clear()
        positions.forEach { (index, point2D) ->
            statePositions[index] = Offset(point2D.x * JFLAP_TO_DP, point2D.y * JFLAP_TO_DP)
        }
    }
}
