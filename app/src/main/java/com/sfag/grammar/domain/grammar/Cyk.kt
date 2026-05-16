package com.sfag.grammar.domain.grammar

import com.sfag.main.config.Symbols

/**
 * CYK (Cocke-Younger-Kasami) acceptance check for context-free grammars. Converts the grammar to
 * CNF internally, then runs the O(n^3) algorithm. Returns true if the grammar generates the input
 * string.
 */
fun cykAccepts(input: String, originalRules: List<GrammarRule>): Boolean {
    if (input.isEmpty()) {
        val individual =
            originalRules.flatMap { rule -> rule.rhs.split('|').map { GrammarRule(rule.lhs, it) } }
        return canDeriveEmpty("S", individual, mutableSetOf())
    }

    val cnfRules = convertToCnf(originalRules)
    val n = input.length

    // table[i][len] = nonterminals that derive input[i..i+len]
    val table = Array(n) { Array(n) { mutableSetOf<String>() } }

    // Base case: single characters
    for (i in 0 until n) {
        val c = input[i].toString()
        for (rule in cnfRules) {
            if (rule.rhs.size == 1 && rule.rhs[0] == c) {
                table[i][0].add(rule.lhs)
            }
        }
    }

    // Fill for increasing substring lengths
    for (len in 1 until n) {
        for (i in 0 until n - len) {
            for (k in 0 until len) {
                for (rule in cnfRules) {
                    if (
                        rule.rhs.size == 2 &&
                            rule.rhs[0] in table[i][k] &&
                            rule.rhs[1] in table[i + k + 1][len - k - 1]
                    ) {
                        table[i][len].add(rule.lhs)
                    }
                }
            }
        }
    }

    return "S" in table[0][n - 1]
}

private fun canDeriveEmpty(
    nonTerminal: String,
    rules: List<GrammarRule>,
    visited: MutableSet<String>,
): Boolean {
    if (!visited.add(nonTerminal)) {
        return false
    }
    return rules.any { rule ->
        rule.lhs == nonTerminal &&
            (rule.rhs == Symbols.EPSILON ||
                rule.rhs.all { c ->
                    c.isUpperCase() && canDeriveEmpty(c.toString(), rules, visited)
                })
    }
}

// Internal CNF rule: right is either [terminal] or [nonterminal, nonterminal]
private data class CnfRule(val lhs: String, val rhs: List<String>)

private fun convertToCnf(originalRules: List<GrammarRule>): List<CnfRule> {
    var ntCounter = 0

    fun freshNt() = "_X${ntCounter++}"

    // Expand | into individual rules
    var rules =
        originalRules
            .flatMap { rule ->
                rule.rhs.split('|').map { rhs ->
                    if (rhs == Symbols.EPSILON) {
                        CnfRule(rule.lhs, listOf(Symbols.EPSILON))
                    } else {
                        CnfRule(rule.lhs, rhs.map { it.toString() })
                    }
                }
            }
            .toMutableList()

    // Step 1: Eliminate epsilon-productions
    val nullable = mutableSetOf<String>()
    var changed = true
    while (changed) {
        changed = false
        for (rule in rules) {
            if (rule.lhs !in nullable && rule.rhs.all { it == Symbols.EPSILON || it in nullable }) {
                nullable.add(rule.lhs)
                changed = true
            }
        }
    }

    val expanded = mutableListOf<CnfRule>()
    for (rule in rules) {
        if (rule.rhs == listOf(Symbols.EPSILON)) {
            continue
        }
        val nullablePositions = rule.rhs.indices.filter { rule.rhs[it] in nullable }
        for (mask in 0 until (1 shl nullablePositions.size)) {
            val omit = mutableSetOf<Int>()
            for (bit in nullablePositions.indices) {
                if (mask and (1 shl bit) != 0) {
                    omit.add(nullablePositions[bit])
                }
            }
            val newRight = rule.rhs.filterIndexed { index, _ -> index !in omit }
            if (newRight.isNotEmpty()) {
                expanded.add(CnfRule(rule.lhs, newRight))
            }
        }
    }
    rules = expanded.distinct().toMutableList()

    // Step 2: Eliminate unit productions (A -> B)
    changed = true
    while (changed) {
        changed = false
        val toAdd = mutableListOf<CnfRule>()
        val toRemove = mutableListOf<CnfRule>()
        for (rule in rules) {
            if (rule.rhs.size == 1 && rule.rhs[0].length == 1 && rule.rhs[0][0].isUpperCase()) {
                toRemove.add(rule)
                val target = rule.rhs[0]
                for (targetRule in rules) {
                    if (targetRule.lhs == target) {
                        val newRule = CnfRule(rule.lhs, targetRule.rhs)
                        if (newRule !in rules && newRule !in toAdd) {
                            toAdd.add(newRule)
                            changed = true
                        }
                    }
                }
            }
        }
        rules.removeAll(toRemove.toSet())
        rules.addAll(toAdd)
    }

    // Step 3: Isolate terminals in rules with 2+ symbols
    val terminalMap = mutableMapOf<String, String>()
    val terminalRules = mutableListOf<CnfRule>()
    rules =
        rules
            .map { rule ->
                if (rule.rhs.size < 2) {
                    return@map rule
                }
                val newRight =
                    rule.rhs.map { symbol ->
                        if (
                            symbol.length == 1 && (symbol[0].isLowerCase() || symbol[0].isDigit())
                        ) {
                            terminalMap.getOrPut(symbol) {
                                val nonTerminal = freshNt()
                                terminalRules.add(CnfRule(nonTerminal, listOf(symbol)))
                                nonTerminal
                            }
                        } else {
                            symbol
                        }
                    }
                CnfRule(rule.lhs, newRight)
            }
            .toMutableList()
    rules.addAll(terminalRules)

    // Step 4: Break rules with 3+ symbols into binary chains
    val binaryRules = mutableListOf<CnfRule>()
    for (rule in rules) {
        if (rule.rhs.size <= 2) {
            binaryRules.add(rule)
        } else {
            var currentLeft = rule.lhs
            for (i in 0 until rule.rhs.size - 2) {
                val newNonTerminal = freshNt()
                binaryRules.add(CnfRule(currentLeft, listOf(rule.rhs[i], newNonTerminal)))
                currentLeft = newNonTerminal
            }
            binaryRules.add(
                CnfRule(currentLeft, listOf(rule.rhs[rule.rhs.size - 2], rule.rhs.last()))
            )
        }
    }

    return binaryRules.distinct()
}
