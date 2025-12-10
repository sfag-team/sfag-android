package com.sfag.grammar.domain.model.rule

data class GrammarRule(val left: String, val right: String) {
    override fun toString(): String {
        return "$left → $right"
    }
}
