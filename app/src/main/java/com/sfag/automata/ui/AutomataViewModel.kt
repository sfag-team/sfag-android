package com.sfag.automata.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.sfag.automata.data.Storage
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.simulation.Simulation
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.main.data.Point2D
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AutomataViewModel
    @Inject
    internal constructor(
        private val storage: Storage,
    ) : ViewModel() {
        // The single current machine - observed by the Activity to key the composition.
        var currentMachine by mutableStateOf<Machine?>(null)
            private set

        // Layout state - persists across simulation/edit mode switches
        val statePositions: SnapshotStateMap<Int, Offset> = mutableStateMapOf()
        var offsetXCanvas by mutableFloatStateOf(0f)
        var offsetYCanvas by mutableFloatStateOf(0f)
        var scaleCanvas by mutableFloatStateOf(0.5f)
        var machineAutoCenter by mutableStateOf(false)

        /** Sets the active machine, initializes positions, and resets view to default scale + center. */
        fun setCurrentMachine(
            machine: Machine,
            positions: Map<Int, Point2D> = emptyMap(),
        ) {
            currentMachine = machine
            initPositions(positions)
            scaleCanvas = 0.5f
            machineAutoCenter = true
        }

        /** Persists the current machine to the fixed auto-save slot. */
        fun autoSave(machine: Machine) {
            storage.saveMachine(machine, getPositions(), offsetXCanvas, offsetYCanvas, scaleCanvas)
        }

        /** Loads the auto-saved machine. Returns true on success. */
        fun loadMachine(): Boolean {
            val (machine, positions, canvas) = storage.loadMachine() ?: return false
            currentMachine = machine
            initPositions(positions)
            offsetXCanvas = canvas.first
            offsetYCanvas = canvas.second
            scaleCanvas = canvas.third
            return true
        }

        fun advanceSimulation(): Simulation = currentMachine?.advanceSimulation() ?: Simulation.Ended(SimulationOutcome.ACTIVE)

        /** Called when positions are loaded from storage or parsed from JFF. */
        fun initPositions(positions: Map<Int, Point2D>) {
            statePositions.clear()
            positions.forEach { (index, point2D) -> statePositions[index] = Offset(point2D.x, point2D.y) }
        }

        /** Returns current positions for JFF export/save. */
        fun getPositions(): Map<Int, Point2D> = statePositions.mapValues { (_, offset) -> Point2D(offset.x, offset.y) }

        /** Updates a single state's position (called on drag). */
        fun updateStatePosition(
            stateIndex: Int,
            delta: Offset,
        ) {
            statePositions[stateIndex]?.let { statePositions[stateIndex] = it + delta }
        }

        /** Assigns a position to a newly created state. */
        fun addStatePosition(
            stateIndex: Int,
            offset: Offset,
        ) {
            statePositions[stateIndex] = offset
        }
    }
