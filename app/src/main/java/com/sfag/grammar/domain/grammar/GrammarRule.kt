package com.sfag.grammar.domain.grammar

import com.sfag.main.config.Symbols

data class GrammarRule(val lhs: String, val rhs: String) {
    override fun toString(): String = "$lhs ${Symbols.PRODUCTION} $rhs"
}
