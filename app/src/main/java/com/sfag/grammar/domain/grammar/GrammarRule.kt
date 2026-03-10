package com.sfag.grammar.domain.grammar

import com.sfag.main.config.Symbols

data class GrammarRule(
    val left: String,
    val right: String,
) {
    override fun toString(): String = "$left ${Symbols.ARROW} $right"
}
