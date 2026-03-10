package com.sfag.automata.domain.machine

class State(
    val index: Int,
    var name: String,
    var initial: Boolean,
    var final: Boolean,
    var isCurrent: Boolean,
) {
    override fun toString(): String = name
}
