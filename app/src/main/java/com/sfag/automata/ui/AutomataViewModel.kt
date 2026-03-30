package com.sfag.automata.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.sfag.automata.data.AutomataStorage
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.tree.NodeSnapshot
import com.sfag.main.config.INITIAL_ZOOM
import com.sfag.main.data.Point2D
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// JFLAP coordinate conversion (96 DPI desktop to 160 DPI Android dp) - protocol constant
private const val JFLAP_TO_DP = 160f / 96f

@HiltViewModel
class AutomataViewModel
@Inject
internal constructor(
    private val storage: AutomataStorage,
) : ViewModel() {
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
    var inspectedNodeId by mutableStateOf<Int?>(null)
        private set
    var inspectedSnapshot by mutableStateOf<NodeSnapshot?>(null)
        private set

    fun inspectNode(machine: Machine, nodeId: Int) {
        val node = machine.tree.findNode(nodeId)
        inspectedNodeId = nodeId
        inspectedSnapshot = node?.snapshot
    }

    fun clearInspection() {
        inspectedNodeId = null
        inspectedSnapshot = null
    }

    // Track unsaved changes
    var hasUnsavedChanges by mutableStateOf(false)
        private set

    fun markDirty() {
        hasUnsavedChanges = true
    }

    fun markSaved() {
        hasUnsavedChanges = false
    }

    /**
     * Sets the active machine and resets view to default scale + center.
     * JFLAP coordinates (96 DPI) are scaled to dp (160 DPI) via JFLAP_TO_DP.
     */
    fun setCurrentMachine(
        machine: Machine,
        positions: Map<Int, Point2D> = emptyMap(),
    ) {
        currentMachine = machine
        machine.setInitialStateAsCurrent()
        loadPositions(positions)
        scaleCanvas = INITIAL_ZOOM
        machineAutoCenter = true
        markSaved()
    }

    /** Persists the current machine to the fixed auto-save slot. Returns true on success. */
    fun autoSave(machine: Machine): Boolean =
        storage.saveMachine(
            machine,
            getPositions(),
            offsetXCanvas,
            offsetYCanvas,
            scaleCanvas,
            hasUnsavedChanges
        )

    /** Loads the auto-saved machine. Returns true on success. */
    fun loadMachine(): Boolean {
        val stored = storage.loadMachine() ?: return false
        currentMachine = stored.machine
        stored.machine.setInitialStateAsCurrent()
        loadPositions(stored.positions)
        offsetXCanvas = stored.offsetX
        offsetYCanvas = stored.offsetY
        scaleCanvas = stored.scale
        if (stored.dirty) markDirty() else markSaved()
        return true
    }

    fun advanceSimulation(): Simulation =
        currentMachine?.advanceSimulation() ?: Simulation.Ended(SimulationOutcome.ACTIVE)

    /** Returns current positions as JFLAP units for JFF export/save (dp -> JFLAP). */
    fun getPositions(): Map<Int, Point2D> =
        statePositions.mapValues { (_, offset) ->
            Point2D(offset.x / JFLAP_TO_DP, offset.y / JFLAP_TO_DP)
        }

    /** Updates a single state's position (called on drag). */
    fun updateStatePosition(
        stateIndex: Int,
        delta: Offset,
    ) {
        statePositions[stateIndex]?.let {
            statePositions[stateIndex] = it + delta
        }
    }

    /** Assigns a position to a newly created state. */
    fun addStatePosition(
        stateIndex: Int,
        offset: Offset,
    ) {
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
