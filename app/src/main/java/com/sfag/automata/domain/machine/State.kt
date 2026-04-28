package com.sfag.automata.domain.machine

class State(val index: Int, var name: String, var initial: Boolean, var final: Boolean) {
    override fun toString(): String = name
}
