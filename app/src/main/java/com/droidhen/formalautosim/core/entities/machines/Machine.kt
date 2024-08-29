package com.droidhen.formalautosim.core.entities.machines

import com.droidhen.formalautosim.core.entities.states.State
import java.nio.file.Path

abstract class Machine (var name:String = "Untitled") {
    val states = mutableListOf<State>()
    val transitions = mutableListOf<Pair<Int, Int>>()
    val input = ArrayDeque<Char>()
    abstract var currentState:Int?

    abstract fun getTransitionPath(transition:Pair<Int, Int>):Path?

    abstract fun simulateTransition()

    abstract fun converteMachineToKeyValue()

}