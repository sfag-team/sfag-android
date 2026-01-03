package com.sfag.grammar.core.rule

data class GrammarRule(val left: String, val right: String) {
    override fun toString(): String {
        return "$left → $right"
    }
}
