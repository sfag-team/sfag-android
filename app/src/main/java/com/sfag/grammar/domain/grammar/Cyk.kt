package com.sfag.grammar.domain.grammar

import com.sfag.main.config.Symbols

/**
 * CYK (Cocke-Younger-Kasami) acceptance check for context-free grammars. Converts the grammar to
 * CNF internally, then runs the O(n^3) algorithm. Returns true if the grammar generates the input
 * string.
 */
fun cykAccepts(
    input: String,
    originalRules: List<GrammarRule>,
): Boolean {
    if (input.isEmpty()) {
        val individual = originalRules.flatMap { rule ->
            rule.right.split('|').map { GrammarRule(rule.left, it) }
        }
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
            if (rule.right.size == 1 && rule.right[0] == c) {
                table[i][0].add(rule.left)
            }
        }
    }

    // Fill for increasing substring lengths
    for (len in 1 until n) {
        for (i in 0 until n - len) {
            for (k in 0 until len) {
                for (rule in cnfRules) {
                    if (
                        rule.right.size == 2 &&
                        rule.right[0] in table[i][k] &&
                        rule.right[1] in table[i + k + 1][len - k - 1]
                    ) {
                        table[i][len].add(rule.left)
                    }
                }
            }
        }
    }

    return "S" in table[0][n - 1]
}

private fun canDeriveEmpty(
    symbol: String,
    rules: List<GrammarRule>,
    visited: MutableSet<String>,
): Boolean {
    if (!visited.add(symbol)) return false
    return rules.any { rule ->
        rule.left == symbol &&
                (
                        rule.right == Symbols.EPSILON ||
                                rule.right.all { c ->
                                    c.isUpperCase() && canDeriveEmpty(c.toString(), rules, visited)
                                }
                        )
    }
}

// Internal CNF rule: right is either [terminal] or [nonterminal, nonterminal]
private data class CnfRule(
    val left: String,
    val right: List<String>,
)

private fun convertToCnf(originalRules: List<GrammarRule>): List<CnfRule> {
    var nextId = 0

    fun freshNt() = "_X${nextId++}"

    // Expand | into individual rules
    var rules =
        originalRules
            .flatMap { rule ->
                rule.right.split('|').map { rhs ->
                    if (rhs == Symbols.EPSILON) {
                        CnfRule(rule.left, listOf(Symbols.EPSILON))
                    } else {
                        CnfRule(rule.left, rhs.map { it.toString() })
                    }
                }
            }.toMutableList()

    // Step 1: Eliminate epsilon-productions
    val nullable = mutableSetOf<String>()
    var changed = true
    while (changed) {
        changed = false
        for (rule in rules) {
            if (
                rule.left !in nullable && rule.right.all { it == Symbols.EPSILON || it in nullable }
            ) {
                nullable.add(rule.left)
                changed = true
            }
        }
    }

    val expanded = mutableListOf<CnfRule>()
    for (rule in rules) {
        if (rule.right == listOf(Symbols.EPSILON)) continue
        val nullablePositions = rule.right.indices.filter { rule.right[it] in nullable }
        for (mask in 0 until (1 shl nullablePositions.size)) {
            val omit = mutableSetOf<Int>()
            for (bit in nullablePositions.indices) {
                if (mask and (1 shl bit) != 0) omit.add(nullablePositions[bit])
            }
            val newRight = rule.right.filterIndexed { index, _ -> index !in omit }
            if (newRight.isNotEmpty()) {
                expanded.add(CnfRule(rule.left, newRight))
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
            if (
                rule.right.size == 1 && rule.right[0].length == 1 && rule.right[0][0].isUpperCase()
            ) {
                toRemove.add(rule)
                val target = rule.right[0]
                for (targetRule in rules) {
                    if (targetRule.left == target) {
                        val newRule = CnfRule(rule.left, targetRule.right)
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
                if (rule.right.size < 2) return@map rule
                val newRight =
                    rule.right.map { symbol ->
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
                CnfRule(rule.left, newRight)
            }.toMutableList()
    rules.addAll(terminalRules)

    // Step 4: Break rules with 3+ symbols into binary chains
    val binaryRules = mutableListOf<CnfRule>()
    for (rule in rules) {
        if (rule.right.size <= 2) {
            binaryRules.add(rule)
        } else {
            var currentLeft = rule.left
            for (i in 0 until rule.right.size - 2) {
                val newNonTerminal = freshNt()
                binaryRules.add(CnfRule(currentLeft, listOf(rule.right[i], newNonTerminal)))
                currentLeft = newNonTerminal
            }
            binaryRules.add(
                CnfRule(currentLeft, listOf(rule.right[rule.right.size - 2], rule.right.last())),
            )
        }
    }

    return binaryRules.distinct()
}
