package com.sfag.automata.presentation.viewmodel

import android.annotation.SuppressLint
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
 * Call clear() when navigating away to prevent memory leaks.
 */
internal object CurrentMachine {
    var machine: Machine? = null
        set(value) {
            // Clear context references from previous machine
            field?.clearContextReferences()
            field = value
        }

    fun clear() {
        machine?.clearContextReferences()
        machine = null
    }
}
