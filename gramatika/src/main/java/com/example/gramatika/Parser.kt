package com.example.gramatika

import kotlin.collections.ArrayDeque
import kotlin.math.exp

//data class State(val stateString: String, val appliedRule: GrammarRule, val previousState: State?)

//fun parse(input: String, inRules: List<GrammarRule>, terminals: Set<Char>): List<State>? {
//    if(input.any { it !in terminals }){
//        return null
//    }
//    val rules = mutableListOf<GrammarRule>()
//
//    for(rule in inRules){
//        for(r in rule.right.split('|')){
//            rules.add(GrammarRule(rule.left, r))
//        }
//    }
//
//    val states = ArrayDeque<State>()
//    val possibleStates = mutableSetOf<String>()
//
//    // Initialize the queue with rules where the left side is "S"
//    rules.filter { it.left == "S" }.forEach { rule ->
//        val initialState = State(rule.right, rule, null)
//        states.addLast(initialState)
//        possibleStates.add(rule.right)
//    }
//    val maxDepth = input.length * (10.0).pow(rules.size) //
//    var steps = 0
//    // Process the queue
//    while (states.isNotEmpty()) {
//        if (steps > maxDepth) {
//            return null
//        }
//        val currentState = states.removeFirst()
//        steps++
//
//        // For each rule, apply it to generate new states
//        for (rule in rules) {
//
//            if (currentState.stateString.contains(rule.left)) {
//                val newState = currentState.stateString.replaceFirst(rule.left, rule.right.replace("ε",""))
//                if(newState == input){
//                    return reconstructDerivation(State(newState, rule, currentState))
//                }
//                if(newState.any { it.isUpperCase() }){
//                    if (newState !in possibleStates) {
//                        val newStateObj = State(newState, rule, currentState)
//                        states.addLast(newStateObj)
//                        possibleStates.add(newState)
//                    }
//                }
//            }
//        }
//    }
//    // If no match is found
//    return null
//}

// Function to reconstruct the derivation by backtracking
//fun reconstructDerivation(finalState: State): List<State> {
//    var currentState: State? = finalState
//    val derivationSteps = mutableListOf<State>()
//
//    // Backtrack through the history
//    while (currentState != null) {
//        derivationSteps.add(currentState)
//        currentState = currentState.previousState
//    }
//
//    return derivationSteps.reversed()
//}

data class State(val stateString: String, val appliedRule: GrammarRule)

fun parse(input: String, rules: List<GrammarRule>, terminals: Set<Char>, type: GrammarType): List<Step>? {
    if (input == "") {
        val hasEpsilon = rules.any { it.left == "S" && it.right == "ε" }
        if (hasEpsilon) {
            return listOf(Step("S", "ε", GrammarRule("S", "ε")))
        } else {
            return null
        }
    }

    if (input.any { it !in terminals }) {
        return null
    }

    val states = ArrayDeque<String>()
//    val possibleStates = mutableSetOf<String>()
    val stateHistory = mutableMapOf<String, State>()

    // Initialize the queue with rules where the left side is "S"
    rules.filter { it.left == "S" }.forEach { rule ->
        if(rule.right == input){
            stateHistory[rule.right] = State("S", rule)
            return reconstructDerivation(rule.right, stateHistory)
        }else{
            states.add(rule.right)
            stateHistory[rule.right] = State("S", rule)
        }
    }

    val maxDepth = (100*exp(0.5*input.length)).toInt()
    var steps = 0
    // Process the queue
    while (states.isNotEmpty()) {
        if (steps > maxDepth) {
            return null
        }
        val currentState = states.removeFirst()
        steps++

        // For each rule, apply it to generate new states
        if(!(type == GrammarType.UNRESTRICTED || type == GrammarType.CONTEXT_SENSITIVE)){
            for (rule in rules) {
                if (currentState.replace("ε","").contains(rule.left)) {
                    val newState = currentState.replaceFirst(rule.left, rule.right)
                    if(newState.replace("ε","") == input){
                        stateHistory[newState] = State(currentState, rule)
                        return reconstructDerivation(newState, stateHistory)
                    }
                    if(!(rules.any{newState.replace("ε","").contains(it.left)})){ continue
                    }else if (!stateHistory.containsKey(newState)) {

                            val terminalPart = extractLargestTerminalSubstring(newState.replace("ε",""))

                            if (!input.contains(terminalPart)) {
                                continue // This path can't reach the desired input anymore
                            }
                        }
                        states.add(newState)
                        // Store the derivation step in the history map
                        stateHistory[newState] = State(currentState, rule)
                }
            }
        }else{
            for (rule in rules) {
                if (currentState.replace("ε","").contains(rule.left)) {
                    val newStates = mutableListOf<String>()
                    var index = currentState.replace("ε","").indexOf(rule.left)
                    while (index != -1) {
                        val newState = currentState.substring(0, index) +
                                rule.right +
                                currentState.substring(index + rule.left.length)
                        if(newState.replace("ε","") == input){
                            stateHistory[newState] = State(currentState, rule)
                            return reconstructDerivation(newState, stateHistory)
                        }
                        if(!(rules.any{newState.replace("ε","").contains(it.left)})){
                            index = currentState.replace("ε","").indexOf(rule.left, index + 1)
                            continue }
                        newStates.add(newState)
                        index = currentState.replace("ε","").indexOf(rule.left, index + 1) // Find next occurrence
                    }

                    // Add all new states to queue
                    for (state in newStates) {
                        if (!stateHistory.containsKey(state)) {
                            states.add(state)
                            stateHistory[state] = State(currentState, rule)
                        }
                    }
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