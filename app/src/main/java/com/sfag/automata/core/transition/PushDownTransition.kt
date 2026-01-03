package com.sfag.automata.core.transition

data class PushDownTransition(
    override var name: String,
    override var startState: Int,
    override var endState: Int,
    var pop: String,
    var push: String
) : Transition(name, startState, endState)
