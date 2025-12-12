package com.sfag.grammar.domain.usecase.parser

import com.sfag.grammar.domain.model.rule.GrammarRule
import com.sfag.grammar.domain.model.type.GrammarType
import com.sfag.shared.util.Symbols
import kotlin.collections.ArrayDeque
import kotlin.math.exp


data class State(val stateString: String, val appliedRule: GrammarRule)
fun parse(input: String, rules: List<GrammarRule>, terminals: Set<Char>, type: GrammarType): List<Step>? {
    if (input != "" && input.any { it !in terminals }) {
        return null
    }

    val states = ArrayDeque<String>()
    val stateHistory = mutableMapOf<String, State>()

    // Init with S
    rules.filter { it.left == "S" }.forEach { rule ->
        states.add(rule.right)
        stateHistory[rule.right] = State("S", rule)
    }

    val maxSteps = (100*exp(0.7*input.length)).toInt()
    var steps = 0
    // Process the queue
    if (type == GrammarType.UNRESTRICTED || type == GrammarType.CONTEXT_SENSITIVE) {
        // Handle unrestricted/context-sensitive
        while (states.isNotEmpty() && steps <= maxSteps) {
            val currentState = states.removeFirst()
            steps++
            for (rule in rules) {
                val newState = currentState.replaceFirst(rule.left, rule.right)
                if (newState.replace(Symbols.EPSILON, "") == input) {
                    stateHistory[newState] = State(currentState, rule)
                    return reconstructDerivation(newState, stateHistory)
                }
                if (!rules.any { newState.replace(Symbols.EPSILON, "").contains(it.left) }) {
                    continue
                }
                if (!stateHistory.containsKey(newState)) {
                    states.add(newState)
                    stateHistory[newState] = State(currentState, rule)
                }
            }
        }
    } else {
        // Handle context-free/regular
        while (states.isNotEmpty() && steps <= maxSteps) {
            val currentState = states.removeFirst()
            steps++
            for (rule in rules) {
                val newState = currentState.replaceFirst(rule.left, rule.right)

                if (newState.replace(Symbols.EPSILON, "") == input) {
                    if(newState != currentState)
                        stateHistory[newState] = State(currentState, rule)
                    return reconstructDerivation(newState, stateHistory)
                }

                if (!rules.any { newState.replace(Symbols.EPSILON, "").contains(it.left) }) continue

                if (!stateHistory.containsKey(newState)) {
                    val terminalPart = extractLargestTerminalSubstring(newState.replace(Symbols.EPSILON, ""))
                    if (!input.contains(terminalPart)) continue
                    states.add(newState)
                    stateHistory[newState] = State(currentState, rule)
                }
            }
        }
    }
    // If no match is found
    return null
}

data class Step(val previous: String, val stateString: String, val appliedRule: GrammarRule)

fun reconstructDerivation(finalState: String, stateHistory: Map<String, State>): List<Step> {
    val derivationSteps = mutableListOf<Step>()
    var currentState = finalState

    // Backtrack through the history of states
    while (currentState != "S") {
        val step = stateHistory[currentState] ?: break
        derivationSteps.add(Step(step.stateString, currentState, step.appliedRule))
        currentState = step.stateString
    }
    return derivationSteps.reversed()  // Correct order
}

fun extractLargestTerminalSubstring(state: String): String {
    var maxSubstring = ""
    var currentSubstring = ""

    for (char in state) {
        if (char.isLowerCase() || char.isDigit()) {
            currentSubstring += char
        } else {
            if (currentSubstring.length > maxSubstring.length) {
                maxSubstring = currentSubstring
            }
            currentSubstring = ""
        }
    }

    if (currentSubstring.length > maxSubstring.length) {
        maxSubstring = currentSubstring
    }

    return maxSubstring
}
