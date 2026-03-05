package com.sfag.automata.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import com.sfag.automata.data.AutomataFileStorage
import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.machine.Vec2
import com.sfag.automata.model.simulation.SimulationOutcome
import com.sfag.automata.model.simulation.SimulationStep

@HiltViewModel
class AutomataViewModel @Inject internal constructor(private val storage: AutomataFileStorage) : ViewModel() {

    // The single current machine — observed by the Activity to key the composition.
    var currentMachine by mutableStateOf<Machine?>(null)
        private set

    // Layout state — persists across simulation/edit mode switches
    val statePositions: SnapshotStateMap<Int, Offset> = mutableStateMapOf()
    var scaleGraph by mutableFloatStateOf(1f)
    var offsetXGraph by mutableFloatStateOf(0f)
    var offsetYGraph by mutableFloatStateOf(0f)
    var needsAutoFit by mutableStateOf(true)

    /** Sets the active machine and initializes layout state. */
    fun setCurrentMachine(machine: Machine, positions: Map<Int, Vec2> = emptyMap()) {
        currentMachine = machine
        initPositions(positions)
    }

    /** Persists the current machine to the fixed auto-save slot. */
    fun autoSave(machine: Machine) {
        storage.saveCurrentMachine(machine, getPositionsAsVec2())
    }

    /** Loads the auto-saved machine. Returns true on success. */
    fun loadCurrentMachine(): Boolean {
        val result = storage.loadCurrentMachine() ?: return false
        val (machine, positions) = result
        setCurrentMachine(machine, positions)
        return true
    }

    fun calculateNextStep(): SimulationStep =
        currentMachine?.calculateNextStep() ?: SimulationStep.Ended(SimulationOutcome.ACTIVE)

    /** Called when positions are loaded from storage or parsed from JFF. */
    fun initPositions(positions: Map<Int, Vec2>) {
        statePositions.clear()
        positions.forEach { (idx, vec2) -> statePositions[idx] = Offset(vec2.x, vec2.y) }
        needsAutoFit = true
        scaleGraph = 1f
        offsetXGraph = 0f
        offsetYGraph = 0f
    }

    /** Returns current positions as a Vec2 map for JFF export/save. */
    fun getPositionsAsVec2(): Map<Int, Vec2> =
        statePositions.mapValues { (_, offset) -> Vec2(offset.x, offset.y) }

    /** Updates a single state's position (called on drag). */
    fun updateStatePosition(stateIndex: Int, delta: Offset) {
        statePositions[stateIndex]?.let { statePositions[stateIndex] = it + delta }
    }

    /** Assigns a position to a newly created state. */
    fun addStatePosition(stateIndex: Int, offset: Offset) {
        statePositions[stateIndex] = offset
    }
}
