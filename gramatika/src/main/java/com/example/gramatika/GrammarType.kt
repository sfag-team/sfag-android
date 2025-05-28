package com.example.gramatika

enum class GrammarType(val priority: Int, private val displayName: String) {
    REGULAR(0, "Regular Grammar"),
    CONTEXT_FREE(1, "Context-Free Grammar"),
    CONTEXT_SENSITIVE(2, "Context-Sensitive Grammar"),
    UNRESTRICTED(3, "Unrestricted Grammar");

    override fun toString(): String = displayName
}

fun isRegular(rule: GrammarRule): Boolean {
    if (rule.left.length == 1 && rule.left.first().isUpperCase()) {
        if (rule.right.length <= 2) {
            if (rule.right.length == 1 && (rule.right.first().isLowerCase() || rule.right.first().isDigit())) {
                return true
            } else if (rule.right.length == 2 &&
                rule.right[0].isUpperCase().xor(rule.right[1].isUpperCase())) {
                return true
            }
        }
    }
    return false
}

fun isContextFree(rule: GrammarRule): Boolean {
    return rule.left.length == 1 && rule.left.first().isUpperCase()
}

fun isContextSensitive(rule: GrammarRule): Boolean {
    return rule.right.length >= rule.left.length
}