package com.sfag.automata.domain.model.state

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
