package com.droidhen.formalautosim.core.entities.machines

import com.droidhen.formalautosim.core.entities.states.State
import java.nio.file.Path

class FiniteMachine() : Machine() {

    override var currentState: Int? = null


    override fun getTransitionPath(transition: Pair<Int, Int>): Path? {
        TODO("Not yet implemented")
    }

    override fun simulateTransition() {
        TODO("Not yet implemented")
    }

    override fun converteMachineToKeyValue() {
        TODO("Not yet implemented")
    }

    fun addNewState(state: State){
        if(state.initial&&currentState==null){
            currentState = state.index
        }
    }

}