package com.droidhen.formalautosim.core.entities.machines

import androidx.compose.runtime.Composable
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition

class FiniteMachine(name: String = "Untitled") : Machine(name) {

    override var currentState: Int? = null


    @Composable
    override fun calculateTransition(onAnimationEnd: () -> Unit) {
        if (currentState == null) return
        val startState = getStateByIndex(currentState!!)
        val appropriateTransition = getListOfAppropriateTransitions(startState)
        if (appropriateTransition.isEmpty()) return
        val endState = getStateByIndex(appropriateTransition[0].endState)
        val newInputValue = input.removePrefix(appropriateTransition[0].name).toString()
        input.clear()
        input.append(newInputValue)

        AnimationOfTransition(
            start = startState.position,
            end = endState.position,
            radius = startState.radius,
            duration = 3000,
            onAnimationEnd = {
                startState.isCurrent = false
                endState.isCurrent = true
                currentState = endState.index
                onAnimationEnd()
            }
        )
    }


    override fun convertMachineToKeyValue(): List<Pair<String, String>> {
        TODO("Not yet implemented")
    }

    fun addNewState(state: State) {
        if (state.initial && currentState == null) {
            currentState = state.index
        }
        states.add(state)
    }


    private fun getListOfAppropriateTransitions(startState: State): List<Transition> {
        return transitions.filter {
            it.startState == startState.index && input.startsWith(it.name)
        }
    }
}