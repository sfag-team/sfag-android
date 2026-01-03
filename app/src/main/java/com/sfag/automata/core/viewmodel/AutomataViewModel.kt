package com.sfag.automata.core.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.sfag.automata.core.machine.Machine
import com.sfag.automata.data.local.AutomataFileStorage
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

@SuppressLint("StaticFieldLeak")
internal object CurrentMachine {
    var machine: Machine? = null
}
