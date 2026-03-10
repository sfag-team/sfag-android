package com.sfag.grammar.domain.grammar

import com.sfag.main.config.Symbols

fun classifyGrammar(rules: List<GrammarRule>): GrammarType {
    val individual = rules.flatMap { rule -> rule.right.split('|').map { GrammarRule(rule.left, it) } }
    if (individual.isEmpty()) return GrammarType.REGULAR

    // Try regular: each rule must pass isRegular, and linearity must be consistent
    if (individual.all { isRegular(it) }) {
        val hasRight = individual.any { it.right.matches(Regex("^[a-z\\d]+[A-Z]$")) }
        val hasLeft = individual.any { it.right.matches(Regex("^[A-Z][a-z\\d]+$")) }
        if (!hasRight || !hasLeft) {
            // Epsilon only allowed for start symbol S, and S must not appear in any RHS
            val epsRules = individual.filter { it.right == Symbols.EPSILON }
            val epsValid = epsRules.isEmpty() ||
                (epsRules.all { it.left == "S" } && individual.none { 'S' in it.right })
            if (epsValid) return GrammarType.REGULAR
        }
        // Mixed linearity or invalid epsilon - not regular, fall through to context-free
    }

    if (individual.all { isContextFree(it) }) return GrammarType.CONTEXT_FREE

    if (individual.all { isContextSensitive(it) }) {
        // S -> ε requires S to not appear in any RHS
        val hasStartEpsilon = individual.any { it.left == "S" && it.right == Symbols.EPSILON }
        if (hasStartEpsilon && individual.any { it.right.contains('S') }) {
            return GrammarType.UNRESTRICTED
        }
        return GrammarType.CONTEXT_SENSITIVE
    }

    return GrammarType.UNRESTRICTED
}

private fun isRegular(rule: GrammarRule): Boolean {
    if (rule.left.length == 1 && rule.left.first().isUpperCase()) {
        if (
            rule.right == Symbols.EPSILON ||
            rule.right.matches(Regex("^[A-Z]$")) ||
            rule.right.matches(Regex("^[a-z\\d]+$")) ||
            rule.right.matches(Regex("^[a-z\\d]+[A-Z]$")) ||
            rule.right.matches(Regex("^[A-Z][a-z\\d]+$"))
        ) {
            return true
        }
    }
    return false
}

private fun isContextFree(rule: GrammarRule): Boolean = rule.left.length == 1 && rule.left.first().isUpperCase()

private fun isContextSensitive(rule: GrammarRule): Boolean {
    if (rule.right == Symbols.EPSILON && rule.left == "S") return true
    return rule.right.length >= rule.left.length
}
