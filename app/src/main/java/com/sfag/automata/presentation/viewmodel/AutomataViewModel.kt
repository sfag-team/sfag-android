package com.sfag.automata.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.data.AutomataFileStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AutomataViewModel @Inject internal constructor(private val storage: AutomataFileStorage) : ViewModel() {

    fun getAllMachinesName(): List<String> = storage.getAllMachineNames()

    fun getMachineByName(name: String): Machine? = storage.getMachineByName(name)

    fun saveMachine(machine: Machine) {
        storage.saveMachine(machine)
    }
}

/**
 * Temporary holder for the current machine being edited/viewed.
 */
internal object CurrentMachine {
    var machine: Machine? = null
        set(value) {
            // Clear context references from previous machine
            field?.clearDensity()
            field = value
        }
}
