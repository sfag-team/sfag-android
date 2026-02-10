package com.sfag.automata.domain.model.machine

enum class AcceptanceCriteria(val text: String) {
    BY_FINITE_STATE("the finite state"),
    BY_INITIAL_STACK("the initial stack (\"Z\")")
}
