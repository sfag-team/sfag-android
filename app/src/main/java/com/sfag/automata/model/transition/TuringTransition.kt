package com.sfag.automata.model.transition

data class TuringTransition(
    override var name: String,  // read symbol
    override var startState: Int,
    override var endState: Int,
    var writeSymbol: Char,
    var direction: TapeDirection
) : Transition(name, startState, endState)
