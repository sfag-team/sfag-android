package com.droidhen.formalautosim.core.entities.machines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition

class FiniteMachine(
    name: String = "Untitled", version: Int = 1, states: MutableList<State> = mutableListOf(),
    transitions: MutableList<Transition> = mutableListOf(), savedInputs: MutableList<StringBuilder> = mutableListOf()
) : Machine(
    name, version,
    machineType = MachineType.Finite,
    states, transitions, savedInputs = savedInputs
) {
    override var currentState: Int? = null

    @Composable
    override fun calculateTransition(onAnimationEnd: () -> Unit) {
        if (currentState == null) return

        val startState = getStateByIndex(currentState!!)
        val possibleTransitions = getListOfAppropriateTransitions(startState)
        if (possibleTransitions.isEmpty()) return

        var validTransition: Transition? = null

        validTransition = possibleTransitions.firstOrNull { transition ->
            val nextInput = input.removePrefix(transition.name)
            val nextState = getStateByIndex(transition.endState)

            val previousCurrent = currentState
            states.forEach { it.isCurrent = false }
            nextState.isCurrent = true
            currentState = nextState.index

            val result = canReachFinalState(StringBuilder(nextInput))

            nextState.isCurrent = false
            getStateByIndex(previousCurrent!!).isCurrent = true
            currentState = previousCurrent

            result
        }

        if (validTransition == null) {
            validTransition = possibleTransitions.first()
        }

        val endState = getStateByIndex(validTransition.endState)
        val newInputValue = input.removePrefix(validTransition.name).toString()
        input.clear()
        input.append(newInputValue)

        currentTreePosition++
        AnimationOfTransition(
            start = startState.position,
            end = endState.position,
            radius = startState.radius,
            duration = 3000,
            onAnimationEnd = {
                startState.isCurrent = false
                endState.isCurrent = true
                currentState = endState.index
                onAnimationEnd()
            }
        )
    }



    override fun convertMachineToKeyValue(): List<Pair<String, String>> {
        TODO("not yet")
    }

    override fun addNewState(state: State) {
        if (state.initial && currentState == null) {
            currentState = state.index
            state.isCurrent = true
        }
        states.add(state)
    }

    /**
     * Creates list of map that represents tree
     * Each map it's a level of the tree, Key - name of transition, Float - number of leaves under this state
     *
     */
    override fun getDerivationTreeElements(): List<List<TreeNode>> {
        val allPaths = mutableListOf<List<String?>>()

        data class Path(
            val history: List<String?>,
            val currentState: State?,
            val inputIndex: Int,
            val alive: Boolean
        )

        val startStates = states.filter { it.initial }
        var paths = startStates.map {
            Path(listOf(null), it, 0, true)
        }.toMutableList()

        while (paths.any { it.alive }) {
            val nextPaths = mutableListOf<Path>()

            paths.forEach { path ->
                if (!path.alive) {
                    nextPaths.add(Path(path.history + null, null, path.inputIndex, false))
                    return@forEach
                }

                if (path.inputIndex == imuInput.length) {
                    allPaths.add(path.history + path.currentState?.name)
                    nextPaths.add(Path(path.history + null, null, path.inputIndex, false))
                    return@forEach
                }

                val currentChar = imuInput[path.inputIndex]
                val possibleTransitions = transitions.filter {
                    it.startState == path.currentState?.index && it.name.firstOrNull() == currentChar
                }

                if (possibleTransitions.isEmpty()) {
                    allPaths.add(path.history + path.currentState?.name)
                    nextPaths.add(Path(path.history + null, null, path.inputIndex, false))
                    return@forEach
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    nextPaths.add(
                        Path(
                            path.history + path.currentState?.name,
                            nextState,
                            path.inputIndex + 1,
                            true
                        )
                    )
                }
            }

            paths = nextPaths
        }

        val acceptedPaths = allPaths.filter { path ->
            val last = path.lastOrNull()
            last != null && states.any { it.name == last && it.finite }
        }

        val maxDepth = allPaths.maxOfOrNull { it.size } ?: 0
        val normalizedPaths = allPaths.map { path ->
            buildList {
                addAll(path)
                while (size < maxDepth) add(null)
            }
        }

        val tree = mutableListOf<List<TreeNode>>()

        for (level in 1 until maxDepth) {
            val nodeMap = mutableMapOf<String?, MutableList<Int>>()
            normalizedPaths.forEachIndexed { pathIndex, path ->
                val stateName = path[level]
                if (stateName !in nodeMap) nodeMap[stateName] = mutableListOf()
                nodeMap[stateName]?.add(pathIndex)
            }

            val levelNodes = nodeMap.map { (stateName, indices) ->
                val weight = indices.size.toFloat()
                val isAccepted = indices.any { acceptedPaths.contains(normalizedPaths[it]) }
                val isCurrent = (stateName != null && states.firstOrNull { it.name == stateName }?.isCurrent == true) && currentTreePosition == level

                TreeNode(
                    stateName = stateName,
                    weight = weight,
                    isCurrent = isCurrent,
                    isAccepted = isAccepted
                )
            }

            tree.add(levelNodes)
        }

        return tree
    }

    @Composable
    override fun MathFormat() {
        val initialState = states.firstOrNull { it.initial }?.name ?: "q₀"
        val finalStates = states.filter { it.finite }.joinToString(", ") { it.name }
        val inputAlphabet = transitions.mapNotNull { it.name.firstOrNull() }.toSet().joinToString(", ")

        val deltaList = transitions.joinToString("\n") { t ->
            val fromState = states.find { it.index == t.startState }?.name ?: "?"
            val toState = states.find { it.index == t.endState }?.name ?: "?"
            val readSymbol = if (t.name.isEmpty()) "ε" else t.name
            "δ($fromState, $readSymbol) = $toState"
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text("M = (Q, Σ, δ, q₀, F)", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Q = { ${states.joinToString(", ") { it.name }} }")
            Text("Σ = { $inputAlphabet }")
            Text("q₀ = $initialState")
            Text("F = { $finalStates }")

            Spacer(modifier = Modifier.height(12.dp))
            Text("δ:", fontWeight = FontWeight.Bold)
            Text(deltaList, fontFamily = FontFamily.Monospace)
        }
    }



    override fun canReachFinalState(input: StringBuilder): Boolean {
        data class Path(
            val currentState: State,
            val inputIndex: Int
        )

        var paths = mutableListOf<Path>()
        var startState = states.firstOrNull { it.isCurrent }
        if(startState==null){
            setInitialStateAsCurrent()
            startState = states.firstOrNull { it.isCurrent }
        }
        if (startState != null) {
            paths.add(Path(startState, 0))
        }

        while (paths.isNotEmpty()) {
            val nextPaths = mutableListOf<Path>()

            for (path in paths) {
                if (path.inputIndex == input.length && path.currentState.finite) {
                    return true
                }

                if (path.inputIndex == input.length) continue

                val currentChar = input[path.inputIndex]
                val possibleTransitions = transitions.filter {
                    it.startState == path.currentState.index && it.name.first() == currentChar
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    nextPaths.add(Path(nextState, path.inputIndex + 1))
                }
            }

            paths = nextPaths
        }

        return false
    }

    override fun exportToJFF(): String {
        val builder = StringBuilder()
        builder.appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>""")
        builder.appendLine("<structure>")
        builder.appendLine("    <type>${machineType}</type>")
        builder.appendLine("    <automaton>")


        for (state in states) {
            builder.appendLine("""        <state id="${state.index}" name="${state.name}">""")
            builder.appendLine("""            <x>${state.position.x}</x>""")
            builder.appendLine("""            <y>${state.position.y}</y>""")
            if (state.initial) builder.appendLine("            <initial/>")
            if (state.finite) builder.appendLine("            <final/>")
            builder.appendLine("        </state>")
        }

        for (transition in transitions) {
            builder.appendLine("        <transition>")
            builder.appendLine("            <from>${transition.startState}</from>")
            builder.appendLine("            <to>${transition.endState}</to>")
            builder.appendLine("            <read>${transition.name}</read>")
            builder.appendLine("        </transition>")
        }

        builder.appendLine("    </automaton>")
        builder.appendLine("</structure>")

        return builder.toString()
    }

    private fun getListOfAppropriateTransitions(startState: State): List<Transition> {
        return transitions.filter {
            it.startState == startState.index && input.startsWith(it.name)
        }
    }
}