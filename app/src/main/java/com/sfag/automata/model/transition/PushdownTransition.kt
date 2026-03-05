package com.sfag.automata.model.transition

data class PushdownTransition(
    override var name: String,
    override var startState: Int,
    override var endState: Int,
    var pop: String,
    var push: String
) : Transition(name, startState, endState)
