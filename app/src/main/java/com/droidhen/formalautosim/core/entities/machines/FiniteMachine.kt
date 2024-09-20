package com.droidhen.formalautosim.core.entities.machines

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Density
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition

class FiniteMachine(name: String = "Untitled") : Machine(name) {

    override var currentState: Int? = null

    @Composable
    override fun simulateTransition(onAnimationEnd: () -> Unit) {
        if (currentState == null) return
        val startState = getStateByIndex(currentState!!)
        val appropriateTransition = getListOfAppropriateTransitions(startState)
        val endState = getStateByIndex(appropriateTransition[0].endState)

        if (appropriateTransition.isEmpty()) return

        AnimationOfTransition(
            start = startState.position,
            end = endState.position,
            radius = startState.radius,
            duration = 3000,
            onAnimationEnd = {
                startState.isCurrent = false
                endState.isCurrent = true
                currentState = endState.index
                input.removeSuffix(appropriateTransition[0].name)
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
            it.startState == startState.index && input.endsWith(it.name)
        }
    }
}