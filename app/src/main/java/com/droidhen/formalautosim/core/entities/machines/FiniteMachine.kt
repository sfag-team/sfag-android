package com.droidhen.formalautosim.core.entities.machines

import androidx.compose.runtime.Composable
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition

class FiniteMachine(name:String = "Untitled") : Machine(name) {

    override var currentState: Int? = null

    @Composable
    override fun simulateTransition() {
        if(currentState==null) return
        val startState = getStateByIndex(currentState!!)
        val appropriateTransition = getListOfAppropriateTransitions(startState)
        if(appropriateTransition.isEmpty()) return
        AnimationOfTransition(start =startState.position, end = getStateByIndex(appropriateTransition[0].endState).position)
    }

    override fun convertMachineToKeyValue(): List<Pair<String, String>> {
        TODO("Not yet implemented")
    }

    fun addNewState(state: State){
        if(state.initial&&currentState==null){
            currentState = state.index
        }
        states.add(state)
    }


    private fun getListOfAppropriateTransitions(startState: State):List<Transition>{
        val inputString = StringBuilder()
        input.forEach{inputString.append(it)}
       return transitions.filter { it.startState == startState.index && inputString.toString().endsWith(it.name)}
    }
}