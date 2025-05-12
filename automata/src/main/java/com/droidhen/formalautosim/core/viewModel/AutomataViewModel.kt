package com.droidhen.formalautosim.core.viewModel

import androidx.lifecycle.ViewModel
import com.droidhen.formalautosim.core.entities.machines.Machine
import com.droidhen.formalautosim.data.local.AutomataSQLite
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AutomataViewModel @Inject constructor(private val database: AutomataSQLite) : ViewModel() {


    fun getAllMachinesName(): List<String> = database.getAllMachineNames()

    fun getMachineByName(name: String): Machine? = database.getMachineByName(name)

    fun saveMachine(machine: Machine) {
        database.insertOrUpdateMachine(
            machine.name,
            machine.version,
            machine.machineType.tag,
            machine.exportToJFF(),
            machine.savedInputs.map { it.toString() })
    }
}

object CurrentMachine {
    var machine: Machine? = null
}