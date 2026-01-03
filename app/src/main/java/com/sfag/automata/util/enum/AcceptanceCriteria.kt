package com.sfag.automata.util.enum

enum class AcceptanceCriteria(val text: String) {
    BY_FINITE_STATE("the finite state"),
    BY_INITIAL_STACK("the initial stack (\"Z\")")
}
