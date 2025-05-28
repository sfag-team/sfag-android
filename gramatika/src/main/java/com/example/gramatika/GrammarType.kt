package com.example.gramatika

enum class GrammarType(val priority: Int, private val displayName: String) {
    REGULAR(0, "Regular Grammar"),
    CONTEXT_FREE(1, "Context-Free Grammar"),
    CONTEXT_SENSITIVE(2, "Context-Sensitive Grammar"),
    UNRESTRICTED(3, "Unrestricted Grammar");

    override fun toString(): String = displayName
}

fun isRegular(rule: GrammarRule): Boolean {
    if (rule.left.first().isUpperCase()) {
        if (rule.right == "ε" ||
            rule.right.matches(Regex("^\\d+\$")) ||                    // all digits
            rule.right.matches(Regex("^[a-z]+\$")) ||                  // all lowercase
            rule.right.matches(Regex("^[a-z\\d]*[A-Z]?\$"))            // optional 1 uppercase at end
        ) {
            return true
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