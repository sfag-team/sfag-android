package com.sfag.grammar.domain.grammar

import com.sfag.main.config.MAX_BFS_GRAMMAR_CONFIGS
import com.sfag.main.config.Symbols

// Sentinel for "unreachable / infeasible" minimum length. Half of MAX_VALUE so sums never overflow
private const val INF_LENGTH = Int.MAX_VALUE / 2

private data class ParseState(val previous: String, val appliedRule: GrammarRule)

data class DerivationStep(val previous: String, val derived: String, val appliedRule: GrammarRule)

sealed class ParseResult {
    data class Success(val steps: List<DerivationStep>) : ParseResult()

    data object Rejected : ParseResult()

    data object Inconclusive : ParseResult()
}

/**
 * BFS brute-force parser. Uses CYK O(n³) pre-check for CFG/Regular to reject quickly. Regular
 * grammars are treated as a CFG subset.
 */
fun parse(
    input: String,
    rules: List<GrammarRule>,
    terminals: Set<Char>,
    grammarType: GrammarType,
): ParseResult {
    if (input != "" && input.any { it !in terminals }) {
        return ParseResult.Rejected
    }

    // CYK pre-check for CFG/Regular - reject in O(n³) before BFS
    if (grammarType == GrammarType.CONTEXT_FREE || grammarType == GrammarType.REGULAR) {
        if (!cykAccepts(input, rules)) {
            return ParseResult.Rejected
        }
    }

    val queue = ArrayDeque<String>()
    val history = mutableMapOf<String, ParseState>()

    rules
        .filter { it.lhs == "S" }
        .forEach { rule ->
            val initial = rule.rhs.replace(Symbols.EPSILON, "")
            if (!history.containsKey(initial)) {
                queue.add(initial)
                history[initial] = ParseState("S", rule)
            }
        }

    val minLengths = computeMinLengths(rules)
    var steps = 0

    while (queue.isNotEmpty() && steps <= MAX_BFS_GRAMMAR_CONFIGS) {
        val current = queue.removeFirst()

        // Check if the current sentential form already matches the input
        if (current == input) {
            return ParseResult.Success(reconstructDerivation(current, history))
        }

        steps++
        for (rule in rules) {
            var derived = current.replaceFirst(rule.lhs, rule.rhs)
            if (derived == current && !current.contains(rule.lhs)) {
                continue
            }
            derived = derived.replace(Symbols.EPSILON, "")

            if (derived == input) {
                if (derived != current) {
                    history[derived] = ParseState(current, rule)
                }
                return ParseResult.Success(reconstructDerivation(derived, history))
            }

            if (!rules.any { derived.contains(it.lhs) }) {
                continue
            }

            if (!history.containsKey(derived)) {
                if (minLength(derived, minLengths) > input.length) {
                    continue
                }
                queue.add(derived)
                history[derived] = ParseState(current, rule)
            }
        }
    }
    return if (queue.isEmpty()) ParseResult.Rejected else ParseResult.Inconclusive
}

private fun reconstructDerivation(
    target: String,
    history: Map<String, ParseState>,
): List<DerivationStep> {
    val derivationSteps = mutableListOf<DerivationStep>()
    val visited = mutableSetOf<String>()
    var current = target

    while (current != "S") {
        if (!visited.add(current)) {
            break
        }
        val step = history[current] ?: break
        derivationSteps.add(DerivationStep(step.previous, derived = current, step.appliedRule))
        current = step.previous
    }
    return derivationSteps.reversed()
}

fun findReplacementIndex(rule: GrammarRule, previous: String, derived: String): Int {
    val left = rule.lhs
    val right = rule.rhs.replace(Symbols.EPSILON, "")
    for (i in previous.indices) {
        if (i + left.length <= previous.length && previous.substring(i, i + left.length) == left) {
            val candidate = previous.take(i) + right + previous.substring(i + left.length)
            if (candidate == derived) {
                return i
            }
        }
    }
    return -1
}

/** Precompute the minimum terminal length each nonterminal can derive via fixed-point iteration. */
private fun computeMinLengths(rules: List<GrammarRule>): Map<String, Int> {
    val nonTerminals = rules.map { it.lhs }.toSet()
    val minLengths = nonTerminals.associateWith { INF_LENGTH }.toMutableMap()

    var changed = true
    while (changed) {
        changed = false
        for (rule in rules) {
            val rhs = rule.rhs
            val rhsLength =
                if (rhs == Symbols.EPSILON) {
                    0
                } else {
                    var length = 0
                    for (c in rhs) {
                        length +=
                            if (c.isUpperCase()) {
                                minLengths[c.toString()] ?: INF_LENGTH
                            } else {
                                1
                            }
                        if (length >= INF_LENGTH) {
                            break
                        }
                    }
                    length
                }
            if (rhsLength < (minLengths[rule.lhs] ?: INF_LENGTH)) {
                minLengths[rule.lhs] = rhsLength
                changed = true
            }
        }
    }
    return minLengths
}

/**
 * Compute the minimum possible terminal length of a sentential form. Each terminal counts as 1,
 * each nonterminal counts as its precomputed minimum.
 */
private fun minLength(form: String, minLengths: Map<String, Int>): Int {
    var length = 0
    for (c in form) {
        if (c.toString() == Symbols.EPSILON) {
            continue
        }
        length +=
            if (c.isUpperCase()) {
                minLengths[c.toString()] ?: 1
            } else {
                1
            }
        if (length >= INF_LENGTH) {
            return length
        }
    }
    return length
}
