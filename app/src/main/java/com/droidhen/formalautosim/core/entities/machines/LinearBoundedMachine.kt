package com.droidhen.formalautosim.core.entities.machines

import java.nio.file.Path

class LinearBoundedMachine : Machine() {
    override var currentState: Int?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun getTransitionPath(transition: Pair<Int, Int>): Path? {
        TODO("Not yet implemented")
    }

    override fun simulateTransition() {
        TODO("Not yet implemented")
    }

    override fun converteMachineToKeyValue() {
        TODO("Not yet implemented")
    }
}