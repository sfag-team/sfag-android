package com.sfag.grammar.domain.grammar

enum class GrammarType(private val displayName: String) {
    REGULAR("Regular Grammar"),
    CONTEXT_FREE("Context-Free Grammar"),
    CONTEXT_SENSITIVE("Context-Sensitive Grammar"),
    UNRESTRICTED("Unrestricted Grammar"),
    INVALID("Invalid Grammar");

    override fun toString(): String = displayName
}
