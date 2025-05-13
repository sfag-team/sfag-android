package com.example.gramatika

enum class GrammarType(private val displayName: String) {
    REGULAR("Regular Grammar"),
    CONTEXT_FREE("Context-Free Grammar"),
    CONTEXT_SENSITIVE("Context-Sensitive Grammar"),
    UNRESTRICTED("Unrestricted Grammar");

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