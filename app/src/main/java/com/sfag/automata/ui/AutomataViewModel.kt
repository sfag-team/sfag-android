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
        private val storage: Storage,
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

        /**
         * Sets the active machine and resets view to default scale + center.
         * JFLAP coordinates (96 DPI) are scaled to dp (160 DPI) via JFLAP_TO_DP.
         */
        fun setCurrentMachine(
            machine: Machine,
            positions: Map<Int, Point2D> = emptyMap(),
        ) {
            currentMachine = machine
            loadPositions(positions)
            scaleCanvas = INITIAL_ZOOM
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
            loadPositions(positions)
            offsetXCanvas = canvas.first
            offsetYCanvas = canvas.second
            scaleCanvas = canvas.third
            return true
        }

        fun advanceSimulation(): Simulation = currentMachine?.advanceSimulation() ?: Simulation.Ended(SimulationOutcome.ACTIVE)

        /** Converts JFLAP positions to dp and loads them into statePositions. */
        private fun loadPositions(positions: Map<Int, Point2D>) {
            statePositions.clear()
            positions.forEach { (index, point2D) ->
                statePositions[index] = Offset(point2D.x * JFLAP_TO_DP, point2D.y * JFLAP_TO_DP)
            }
        }

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
