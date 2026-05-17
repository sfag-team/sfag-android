package com.sfag.automata.domain.machine

data class State(val index: Int, val name: String, val initial: Boolean, val final: Boolean) {
    override fun toString(): String = name
}
