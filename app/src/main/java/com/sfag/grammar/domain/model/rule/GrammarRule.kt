package com.sfag.grammar.domain.model.rule

import com.sfag.shared.util.Symbols

data class GrammarRule(val left: String, val right: String) {
    override fun toString(): String {
        return "$left ${Symbols.ARROW} $right"
    }
}
