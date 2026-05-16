package com.sfag.automata.ui

import androidx.compose.runtime.derivedStateOf
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
import com.sfag.automata.domain.tree.NodeStatus
import com.sfag.automata.domain.tree.TreeNode
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
    // The single current machine - observed by the Activity to key the composition
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

    // Pre-computed simulation frames. Null until the user starts stepping
    private var frames by mutableStateOf<List<MachineFrame>?>(null)

    private var currentFrameIndex by mutableIntStateOf(0)

    // Highest frame the user has ever stepped to. Tree expands monotonically to this depth
    // so clicking back to a shallower node doesn't hide already-visible deeper nodes
    private var furthestFrameIndex by mutableIntStateOf(0)

    // Tree node id the user clicked, anchors selectedPath when frames extend
    // Null = path uses default heuristic (accepting > deepest, alphabetical tie-break)
    private var selectionAnchor by mutableStateOf<Int?>(null)

    // Root-to-leaf path of tree node ids indexed by frame depth. Drives Tape/Stack/Tree render
    // Computed over the full frame list so accepting > longest > alphabetical is decided globally
    private val selectedPath: List<Int> by derivedStateOf {
        val list = frames ?: return@derivedStateOf emptyList()
        val anchor = selectionAnchor
        if (anchor != null) {
            computePathThroughNode(list, anchor)
        } else {
            computeSelectedPath(list)
        }
    }

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

    /**
     * Tree node id for Tape/Stack/Tree render. Path entry at current frame, else last path entry
     * (dead branches whose path is shorter than currentFrameIndex), else synthesized initial
     * config's key (before the first step, when no frames are computed yet).
     */
    val selectedNodeId: Int?
        get() =
            selectedPath.getOrNull(currentFrameIndex)
                ?: selectedPath.lastOrNull()
                ?: currentFrame?.activeConfigs?.keys?.firstOrNull()

    /** Config for [selectedNodeId], looking back to the last frame the node was active in. */
    val selectedConfig: Config?
        get() {
            val nodeId = selectedNodeId ?: return null
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

    fun selectNode(nodeId: Int) {
        val depth = currentMachine?.tree?.findNode(nodeId)?.depth ?: return
        if (depth !in 0..furthestFrameIndex) {
            return
        }
        selectionAnchor = nodeId
        currentFrameIndex = depth
    }

    /**
     * Pre-computes frames on first access. Tree shows nodes up to furthestFrameIndex (monotonic).
     */
    private fun ensureFrames(machine: Machine): List<MachineFrame> {
        val cached = frames
        if (cached != null) {
            return cached
        }
        val computed = machine.precomputeFrames()
        frames = computed
        machine.rebuildTreeForFrame(computed, furthestFrameIndex.coerceAtMost(computed.lastIndex))
        return computed
    }

    /**
     * Maintains a sliding window of MAX_SIM_PRECOMPUTE_STEPS frames ahead of currentFrameIndex so
     * playback never runs out of precomputed frames.
     */
    private fun maintainLookahead(machine: Machine) {
        val list = frames ?: return
        if (list.last().outcome != SimulationOutcome.ACTIVE) {
            return
        }
        val targetSteps = currentFrameIndex + MAX_SIM_PRECOMPUTE_STEPS
        if (list.size > targetSteps) {
            return
        }
        val extended = machine.precomputeFrames(targetSteps)
        frames = extended
        machine.rebuildTreeForFrame(extended, furthestFrameIndex.coerceAtMost(extended.lastIndex))
    }

    private fun computeSelectedPath(frames: List<MachineFrame>): List<Int> {
        val rootId = frames.firstOrNull()?.activeConfigs?.keys?.firstOrNull() ?: return emptyList()
        val topology = buildTreeTopology(frames, rootId)
        return pickBestPathFrom(rootId, topology, frames.last().isNodeAccepting)
    }

    /** Builds a root-to-leaf path that passes through [targetNodeId]. Empty if unknown node. */
    private fun computePathThroughNode(frames: List<MachineFrame>, targetNodeId: Int): List<Int> {
        val rootId = frames.firstOrNull()?.activeConfigs?.keys?.firstOrNull() ?: return emptyList()
        val topology = buildTreeTopology(frames, rootId)

        if (targetNodeId != rootId && targetNodeId !in topology.parent) {
            return emptyList()
        }

        // Down: target -> best subtree leaf (accepting > deepest > alphabetical)
        val downPath = pickBestPathFrom(targetNodeId, topology, frames.last().isNodeAccepting)

        // Up: target -> root
        val upPath = ArrayDeque<Int>()
        var upNodeId: Int? = topology.parent[targetNodeId]
        while (upNodeId != null) {
            upPath.addFirst(upNodeId)
            upNodeId = topology.parent[upNodeId]
        }
        return upPath + downPath
    }

    private data class TreeTopology(
        val children: Map<Int, List<Pair<Int, String>>>,
        val parent: Map<Int, Int>,
        val depthOf: Map<Int, Int>,
    )

    private fun buildTreeTopology(frames: List<MachineFrame>, rootId: Int): TreeTopology {
        val children = mutableMapOf<Int, List<Pair<Int, String>>>()
        val parent = mutableMapOf<Int, Int>()
        val depthOf = mutableMapOf(rootId to 0)
        for (frame in frames) {
            for ((parentId, branches) in frame.treeBranches) {
                children[parentId] = branches.map { it.treeNodeId to it.stateName }
                val parentDepth = depthOf[parentId] ?: continue
                for (branch in branches) {
                    depthOf[branch.treeNodeId] = parentDepth + 1
                    parent[branch.treeNodeId] = parentId
                }
            }
        }
        return TreeTopology(children, parent, depthOf)
    }

    /**
     * Walks from [startNodeId] to the best leaf in its subtree, ranked accepting > deepest >
     * alphabetical (tie-break at each branching point).
     */
    private fun pickBestPathFrom(
        startNodeId: Int,
        topology: TreeTopology,
        isAccepting: ((Int) -> Boolean)?,
    ): List<Int> {
        val (children, _, depthOf) = topology

        // Subtree leaves of startNodeId via BFS
        val subtreeLeaves = mutableListOf<Int>()
        val queue = ArrayDeque<Int>()
        queue.add(startNodeId)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val kids = children[node]
            if (kids.isNullOrEmpty()) {
                subtreeLeaves.add(node)
            } else {
                queue.addAll(kids.map { it.first })
            }
        }

        val accepting = isAccepting?.let { fn -> subtreeLeaves.filter(fn).toSet() } ?: emptySet()
        val pool = accepting.ifEmpty { subtreeLeaves.toSet() }
        val maxDepth = pool.maxOf { depthOf.getValue(it) }
        val candidates: Set<Int> = pool.filter { depthOf.getValue(it) == maxDepth }.toSet()

        val containsCandidate = mutableMapOf<Int, Boolean>()
        fun has(id: Int): Boolean {
            containsCandidate[id]?.let {
                return it
            }
            val result = id in candidates || (children[id]?.any { has(it.first) } == true)
            containsCandidate[id] = result
            return result
        }

        val path = mutableListOf<Int>()
        var nodeId = startNodeId
        while (true) {
            path.add(nodeId)
            if (nodeId in candidates) {
                break
            }
            val (nextNodeId, _) =
                children[nodeId]?.filter { has(it.first) }?.minByOrNull { it.second } ?: break
            nodeId = nextNodeId
        }
        return path
    }

    /**
     * Returns the frame that [stepForward] would advance into. Null if simulation cannot extend.
     */
    fun peekNextFrame(machine: Machine): MachineFrame? {
        val list = ensureFrames(machine)
        return list.getOrNull(furthestFrameIndex + 1)
    }

    /**
     * Advances/rewinds playback. Tree grows monotonically with furthestFrameIndex, never shrinks.
     * When the tree grows, the anchor is dropped if its subtree has no viable leaf while the global
     * tree still does.
     */
    fun setFrameIndex(machine: Machine, newIndex: Int) {
        val list = ensureFrames(machine)
        require(newIndex in list.indices) { "frame index $newIndex out of bounds" }
        currentFrameIndex = newIndex
        if (newIndex > furthestFrameIndex) {
            furthestFrameIndex = newIndex
            machine.rebuildTreeForFrame(list, furthestFrameIndex)
            val anchorNode = selectionAnchor?.let { machine.tree.findNode(it) }
            if (
                anchorNode != null &&
                    !anchorNode.hasViableLeaf() &&
                    machine.tree.root?.hasViableLeaf() == true
            ) {
                selectionAnchor = null
            }
        }
    }

    /** Extends the simulation by one step beyond the furthest frame ever reached. */
    fun stepForward(machine: Machine) {
        if (peekNextFrame(machine) != null) {
            setFrameIndex(machine, furthestFrameIndex + 1)
            maintainLookahead(machine)
        }
    }

    private fun TreeNode.hasViableLeaf(): Boolean {
        if (status == NodeStatus.ACCEPTED) {
            return true
        }
        if (children.isEmpty()) {
            return status == NodeStatus.ACTIVE
        }
        return children.any { it.hasViableLeaf() }
    }

    /** Resets playback. Call when machine structure or input changes. */
    fun invalidateSimulation(machine: Machine) {
        clearSimulationState()
        machine.resetToInitialState()
    }

    private fun clearSimulationState() {
        frames = null
        currentFrameIndex = 0
        furthestFrameIndex = 0
        selectionAnchor = null
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
