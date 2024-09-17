package com.droidhen.formalautosim.core.entities.machines

import androidx.compose.runtime.Composable

class TuringMachine: Machine() {
    override var currentState: Int?
        get() = TODO("Not yet implemented")
        set(value) {}



    @Composable
    override fun simulateTransition() {
        TODO("Not yet implemented")
    }

    override fun convertMachineToKeyValue(): List<Pair<String, String>> {
        TODO("Not yet implemented")
    }
}