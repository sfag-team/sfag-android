package com.sfag.grammar.domain.usecase.validation

import com.sfag.grammar.domain.model.rule.GrammarRule
import com.sfag.shared.util.Symbols

fun isRegular(rule: GrammarRule): Boolean {
    if (rule.left.first().isUpperCase()) {
        if (rule.right == Symbols.EPSILON ||
            rule.right.matches(Regex("^\\d+$")) ||                    // all digits
            rule.right.matches(Regex("^[a-z]+$")) ||                  // all lowercase
            rule.right.matches(Regex("^[a-z\\d]*[A-Z]?$"))            // optional 1 uppercase at end
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
