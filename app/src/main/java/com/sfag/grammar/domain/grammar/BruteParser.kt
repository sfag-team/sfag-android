package com.sfag.grammar.domain.grammar

import com.sfag.main.config.Symbols

private const val MAX_BFS_STEPS = 1_000_000

private data class ParseState(
    val previousState: String,
    val appliedRule: GrammarRule,
)

data class DerivationStep(
    val previous: String,
    val derived: String,
    val appliedRule: GrammarRule,
)

sealed class ParseResult {
    data class Success(
        val steps: List<DerivationStep>,
    ) : ParseResult()

    data object Rejected : ParseResult()

    data object Inconclusive : ParseResult()
}

/**
 * BFS brute-force parser. Uses CYK O(n³) pre-check for
 * CFG/Regular to reject quickly. Regular grammars are treated as a CFG subset.
 */
fun parse(
    input: String,
    rules: List<GrammarRule>,
    terminals: Set<Char>,
    grammarType: GrammarType,
): ParseResult {
    if (input != "" && input.any { it !in terminals }) return ParseResult.Rejected

    // CYK pre-check for CFG/Regular - reject in O(n³) before BFS
    if (grammarType == GrammarType.CONTEXT_FREE || grammarType == GrammarType.REGULAR) {
        if (!cykAccepts(input, rules)) return ParseResult.Rejected
    }

    val states = ArrayDeque<String>()
    val stateHistory = mutableMapOf<String, ParseState>()

    rules
        .filter { it.left == "S" }
        .forEach { rule ->
            states.add(rule.right)
            stateHistory[rule.right] = ParseState("S", rule)
        }

    val minLengths = computeMinLengths(rules)
    val maxSteps = MAX_BFS_STEPS
    var steps = 0

    while (states.isNotEmpty() && steps <= maxSteps) {
        val currentState = states.removeFirst()
        steps++
        for (rule in rules) {
            var newState = currentState.replaceFirst(rule.left, rule.right)
            if (newState == currentState && !currentState.contains(rule.left)) continue
            newState = newState.replace(Symbols.EPSILON, "")

            if (newState == input) {
                if (newState != currentState) {
                    stateHistory[newState] = ParseState(currentState, rule)
                }
                return ParseResult.Success(reconstructDerivation(newState, stateHistory))
            }

            if (!rules.any { newState.contains(it.left) }) continue

            if (!stateHistory.containsKey(newState)) {
                if (minimumLength(newState, minLengths) > input.length) continue
                states.add(newState)
                stateHistory[newState] = ParseState(currentState, rule)
            }
        }
    }
    return if (states.isEmpty()) ParseResult.Rejected else ParseResult.Inconclusive
}

private fun reconstructDerivation(
    finalState: String,
    stateHistory: Map<String, ParseState>,
): List<DerivationStep> {
    val derivationSteps = mutableListOf<DerivationStep>()
    val visited = mutableSetOf<String>()
    var currentState = finalState

    while (currentState != "S") {
        if (!visited.add(currentState)) break
        val step = stateHistory[currentState] ?: break
        derivationSteps.add(
            DerivationStep(
                step.previousState,
                derived = currentState,
                step.appliedRule
            )
        )
        currentState = step.previousState
    }
    return derivationSteps.reversed()
}

fun findReplacementIndex(
    rule: GrammarRule,
    previous: String,
    current: String,
): Int {
    val left = rule.left
    val right = rule.right.replace(Symbols.EPSILON, "")
    for (i in previous.indices) {
        if (i + left.length <= previous.length && previous.substring(i, i + left.length) == left) {
            val candidate = previous.take(i) + right + previous.substring(i + left.length)
            if (candidate == current) {
                return i
            }
        }
    }
    return -1
}

/**
 * Precompute the minimum terminal length each nonterminal can derive via fixed-point iteration.
 */
private fun computeMinLengths(rules: List<GrammarRule>): Map<String, Int> {
    val nonTerminals = rules.map { it.left }.toSet()
    val minLengths = nonTerminals.associateWith { Int.MAX_VALUE / 2 }.toMutableMap()

    var changed = true
    while (changed) {
        changed = false
        for (rule in rules) {
            val rhs = rule.right
            val rhsMin =
                if (rhs == Symbols.EPSILON) {
                    0
                } else {
                    var sum = 0
                    for (c in rhs) {
                        sum +=
                            if (c.isUpperCase()) {
                                minLengths[c.toString()] ?: (Int.MAX_VALUE / 2)
                            } else {
                                1
                            }
                        if (sum >= Int.MAX_VALUE / 2) break
                    }
                    sum
                }
            if (rhsMin < (minLengths[rule.left] ?: (Int.MAX_VALUE / 2))) {
                minLengths[rule.left] = rhsMin
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
private fun minimumLength(
    derivation: String,
    minLengths: Map<String, Int>,
): Int {
    var sum = 0
    for (c in derivation) {
        if (c.toString() == Symbols.EPSILON) continue
        sum +=
            if (c.isUpperCase()) {
                minLengths[c.toString()] ?: 1
            } else {
                1
            }
        if (sum >= Int.MAX_VALUE / 2) return sum
    }
    return sum
}
