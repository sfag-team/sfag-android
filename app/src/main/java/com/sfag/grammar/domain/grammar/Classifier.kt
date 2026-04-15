package com.sfag.grammar.domain.grammar

import com.sfag.main.config.Symbols

fun classifyGrammar(rules: List<GrammarRule>): GrammarType {
    val individual =
        rules.flatMap { rule ->
            rule.right.split('|').map { GrammarRule(rule.left.trim(), it.trim()) }
        }

    if (individual.isEmpty() || !individual.all(::isValidRule)) {
        return GrammarType.INVALID
    }

    // Regular grammar requires all rules right-linear OR all rules left-linear
    return if (individual.all(::isType3RLRule) || individual.all(::isType3LLRule)) {
        GrammarType.REGULAR
    } else if (individual.all(::isType2Rule)) {
        GrammarType.CONTEXT_FREE
    } else if (individual.all(::isType1Rule)) {
        GrammarType.CONTEXT_SENSITIVE
    } else {
        GrammarType.UNRESTRICTED
    }
}

private fun isTerminal(c: Char): Boolean = c.isLowerCase() || c.isDigit()

private fun isNonTerminal(c: Char): Boolean = c.isUpperCase()

private fun isSingleNonTerminal(value: String): Boolean {
    return value.length == 1 && isNonTerminal(value.first())
}

// Epsilon symbol on RHS is treated as empty string
private fun normalizedRight(right: String): String {
    return if (right == Symbols.EPSILON) "" else right
}

// Valid rule has only terminals and non-terminals, with at least one non-terminal on LHS
private fun isValidRule(rule: GrammarRule): Boolean {
    val allChars = rule.left + normalizedRight(rule.right)
    val validVocabulary = allChars.all { isNonTerminal(it) || isTerminal(it) }

    return validVocabulary && rule.left.any(::isNonTerminal)
}

// Type 3 Right-Linear form is A -> wB or A -> w where w is a (possibly empty) string of terminals
private fun isType3RLRule(rule: GrammarRule): Boolean {
    if (!isSingleNonTerminal(rule.left)) {
        return false
    }

    val right = normalizedRight(rule.right)
    if (right.isEmpty() || right.all(::isTerminal)) {
        return true
    }

    return isNonTerminal(right.last()) && right.dropLast(1).all(::isTerminal)
}

// Type 3 Left-Linear form is A -> Bw or A -> w where w is a (possibly empty) string of terminals
private fun isType3LLRule(rule: GrammarRule): Boolean {
    if (!isSingleNonTerminal(rule.left)) {
        return false
    }

    val right = normalizedRight(rule.right)
    if (right.isEmpty() || right.all(::isTerminal)) {
        return true
    }

    return isNonTerminal(right.first()) && right.drop(1).all(::isTerminal)
}

// Type 2 rule requires the LHS to be a single non-terminal
private fun isType2Rule(rule: GrammarRule): Boolean {
    return isSingleNonTerminal(rule.left)
}

// Type 1 rule requires |LHS| <= |RHS| (epsilon is length 0, so S -> ε is not Type 1)
private fun isType1Rule(rule: GrammarRule): Boolean {
    return rule.left.length <= normalizedRight(rule.right).length
}
