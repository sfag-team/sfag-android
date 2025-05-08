package com.droidhen.formalautosim.core.entities.machines

import androidx.compose.runtime.Composable
import com.droidhen.formalautosim.core.entities.states.State

class TuringMachine: Machine() {
    override var currentState: Int?
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun calculateTransition(onAnimationEnd: () -> Unit) {
        TODO("Not yet implemented")
    }


    override fun convertMachineToKeyValue(): List<Pair<String, String>> {
        TODO("Not yet implemented")
    }

    override fun addNewState(state: State) {
        TODO("Not yet implemented")
    }

    override fun getDerivationTreeElements(): List<Map<String?, Float>> {
        TODO("Not yet implemented")
    }
}