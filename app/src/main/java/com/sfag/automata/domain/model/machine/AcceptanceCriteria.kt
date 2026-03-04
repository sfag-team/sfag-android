package com.sfag.automata.domain.model.machine

enum class AcceptanceCriteria(val text: String) {
    BY_FINAL_STATE("the final state"),
    BY_INITIAL_STACK("the initial stack (\"Z\")")
}
