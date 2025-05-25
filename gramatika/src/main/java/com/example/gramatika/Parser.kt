package com.example.gramatika

import kotlin.collections.ArrayDeque
import kotlin.math.exp

data class State(val stateString: String, val appliedRule: GrammarRule)
fun parse(input: String, rules: List<GrammarRule>, terminals: Set<Char>, type: GrammarType): List<Step>? {
    if (input != "" && input.any { it !in terminals }) {
        return null
    }

    val states = ArrayDeque<String>()
    val stateHistory = mutableMapOf<String, State>()

    // Initialize the queue with rules with the start symbol "S"
    rules.filter { it.left == "S" }.forEach { rule ->
        states.add(rule.right)
        stateHistory[rule.right] = State("S", rule)
    }

    val maxDepth = (100*exp(0.7*input.length)).toInt()
    var steps = 0
    // Process the queue
    if (type == GrammarType.UNRESTRICTED || type == GrammarType.CONTEXT_SENSITIVE) {
        // Handle unrestricted/context-sensitive
        while (states.isNotEmpty()) {
            if (steps > maxDepth) return null
            val currentState = states.removeFirst()
//            val cleanCurrentState = currentState.replace("ε", "")
            steps++

            for (rule in rules) {


                val newState = currentState.replaceFirst(rule.left,rule.right)

                if (newState.replace("ε", "") == input) {
                    stateHistory[newState] = State(currentState, rule)
                    return reconstructDerivation(newState, stateHistory)
                }

                if (!rules.any { newState.replace("ε", "").contains(it.left) }) {

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
        while (states.isNotEmpty()) {
            if (steps > maxDepth) return null
            val currentState = states.removeFirst()

            steps++


            for (rule in rules) {
                val newState = currentState.replaceFirst(rule.left, rule.right)

                if (newState.replace("ε", "") == input) {
                    if(newState != currentState)
                        stateHistory[newState] = State(currentState, rule)
                    return reconstructDerivation(newState, stateHistory)
                }

                if (!rules.any { newState.replace("ε", "").contains(it.left) }) continue

                if (!stateHistory.containsKey(newState)) {
                    val terminalPart = extractLargestTerminalSubstring(newState.replace("ε", ""))
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
    while (currentState != "S") {  // Assuming "S" is the starting state
        val step = stateHistory[currentState]!!
        derivationSteps.add(Step(step.stateString, currentState, step.appliedRule))  // Add the step to the derivation path
        currentState = step.stateString  // Move to the previous state
    }
    return derivationSteps.reversed()  // Return the derivation steps in the correct order
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
            currentSubstring = "" // Reset the current substring when a non-terminal is encountered
        }
    }

    // Check the last substring if the state ends with terminals
    if (currentSubstring.length > maxSubstring.length) {
        maxSubstring = currentSubstring
    }

    return maxSubstring
}