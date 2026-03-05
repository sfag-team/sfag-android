package com.sfag.grammar.model

import com.sfag.shared.Symbols

data class GrammarRule(val left: String, val right: String) {
    override fun toString(): String {
        return "$left ${Symbols.ARROW_STR} $right"
    }
}
