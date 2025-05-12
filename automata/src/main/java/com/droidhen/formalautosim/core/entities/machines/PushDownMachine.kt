package com.droidhen.formalautosim.core.entities.machines

import androidx.compose.runtime.Composable
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition

class PushDownMachine(
    name: String, version: Int =1, states: MutableList<State> = mutableListOf(),
    transitions: MutableList<Transition> = mutableListOf(), savedInputs: MutableList<StringBuilder> = mutableListOf()
) : Machine(
    name, version,
    machineType = MachineType.Pushdown, states, transitions, savedInputs
) {

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

    override fun canReachFinalState(input: StringBuilder): Boolean {
        TODO("Not yet implemented")
    }
}