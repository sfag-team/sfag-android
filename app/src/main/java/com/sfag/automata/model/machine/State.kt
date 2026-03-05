package com.sfag.automata.model.machine

class State(
    var final: Boolean,
    var initial: Boolean,
    var index: Int,
    var name: String,
    var isCurrent: Boolean = false,
) {
    override fun toString(): String {
        return name
    }
}
