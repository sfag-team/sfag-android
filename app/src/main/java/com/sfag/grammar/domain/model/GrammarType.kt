package com.sfag.grammar.domain.model

enum class GrammarType(val priority: Int, private val displayName: String) {
    REGULAR(0, "Regular Grammar"),
    CONTEXT_FREE(1, "Context-Free Grammar"),
    CONTEXT_SENSITIVE(2, "Context-Sensitive Grammar"),
    UNRESTRICTED(3, "Unrestricted Grammar");

    override fun toString(): String = displayName
}
