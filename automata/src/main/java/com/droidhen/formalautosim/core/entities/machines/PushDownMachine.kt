package com.droidhen.formalautosim.core.entities.machines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition

@Suppress("UNCHECKED_CAST")
class PushDownMachine(
    name: String,
    version: Int = 1,
    states: MutableList<State> = mutableListOf(),
    transitions: MutableList<PushDownTransition> = mutableListOf(),
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
    val symbolStack: MutableList<Char> = mutableListOf()
) : Machine(
    name, version,
    machineType = MachineType.Pushdown, states, transitions as MutableList<Transition>, savedInputs = savedInputs
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
            val tempStack = symbolStack.toMutableList()

            if (transition is PushDownTransition) {
                if (transition.pop.isNotEmpty()) {
                    val expectedTop = transition.pop.first()
                    if (tempStack.isEmpty() || tempStack.last() != expectedTop) return@firstOrNull false
                    tempStack.removeLast()
                }
                if (transition.pull.isNotEmpty()) {
                    tempStack.add(transition.pull.first())
                }
            }

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

        if (validTransition is PushDownTransition) {
            if (validTransition.pop.isNotEmpty()) {
                symbolStack.removeLast()
            }
            if (validTransition.pull.isNotEmpty()) {
                symbolStack.add(validTransition.pull.first())
            }
        }
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
        TODO("Not yet implemented")
    }

    override fun addNewState(state: State) {
        if (state.initial && currentState == null) {
            currentState = state.index
            state.isCurrent = true
        }
        states.add(state)
    }

    override fun getDerivationTreeElements(): List<List<TreeNode>> {
        val allPaths = mutableListOf<List<String?>>()

        data class Path(
            val history: List<String?>,
            val currentState: State?,
            val inputIndex: Int,
            val symbolStack: List<Char>,
            val alive: Boolean
        )

        val startStates = states.filter { it.initial }
        var paths = startStates.map {
            Path(listOf(null), it, 0, emptyList(), true)
        }.toMutableList()

        while (paths.any { it.alive }) {
            val nextPaths = mutableListOf<Path>()

            paths.forEach { path ->
                if (!path.alive) {
                    nextPaths.add(path.copy(history = path.history + null, currentState = null, alive = false))
                    return@forEach
                }

                if (path.inputIndex == imuInput.length) {
                    allPaths.add(path.history + path.currentState?.name)
                    nextPaths.add(path.copy(history = path.history + null, currentState = null, alive = false))
                    return@forEach
                }

                val currentChar = imuInput[path.inputIndex]
                val currentStack = path.symbolStack.toMutableList()

                val possibleTransitions = transitions.filter { it.startState == path.currentState?.index }
                    .filter { it.name.firstOrNull() == currentChar }

                if (possibleTransitions.isEmpty()) {
                    allPaths.add(path.history + path.currentState?.name)
                    nextPaths.add(path.copy(history = path.history + null, currentState = null, alive = false))
                    return@forEach
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    val newStack = currentStack.toMutableList()

                    if (transition is PushDownTransition) {
                        // POP
                        if (transition.pop.isNotEmpty()) {
                            val expectedTop = transition.pop.first()
                            if (newStack.isEmpty() || newStack.last() != expectedTop) continue
                            newStack.removeLast()
                        }

                        // PUSH
                        if (transition.pull.isNotEmpty()) {
                            newStack.add(transition.pull.first())
                        }
                    }

                    nextPaths.add(
                        Path(
                            history = path.history + path.currentState?.name,
                            currentState = nextState,
                            inputIndex = path.inputIndex + 1,
                            symbolStack = newStack,
                            alive = true
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

            normalizedPaths.forEachIndexed { index, path ->
                val stateName = path[level]
                nodeMap.computeIfAbsent(stateName) { mutableListOf() }.add(index)
            }

            val levelNodes = nodeMap.map { (stateName, indices) ->
                val weight = indices.size.toFloat()
                val isAccepted = indices.any { acceptedPaths.contains(normalizedPaths[it]) }
                //Не корректно сравнивать по имени
                val isCurrent = (stateName != null && states.firstOrNull { it.name == stateName }?.isCurrent == true)&&currentTreePosition==level

                TreeNode(
                    stateName = stateName,
                    weight = weight,
                    isAccepted = isAccepted,
                    isCurrent = isCurrent
                )
            }

            tree.add(levelNodes)
        }

        return tree
    }



    override fun canReachFinalState(input: StringBuilder): Boolean {
        data class Path(
            val currentState: State,
            val inputIndex: Int,
            val symbolStack: List<Char>
        )

        var startState = states.firstOrNull { it.isCurrent }
        if(startState==null){
            setInitialStateAsCurrent()
            startState = states.firstOrNull { it.isCurrent }
        }
        if(startState==null) return false
        var paths = mutableListOf(Path(startState, 0, emptyList()))

        while (paths.isNotEmpty()) {
            val nextPaths = mutableListOf<Path>()

            for (path in paths) {
                if (path.inputIndex == input.length && path.currentState.finite) {
                    return true
                }

                if (path.inputIndex == input.length) continue

                val currentChar = input[path.inputIndex]
                val possibleTransitions = transitions.filter {
                    it.startState == path.currentState.index &&
                            it.name.firstOrNull() == currentChar
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    val newStack = path.symbolStack.toMutableList()

                    if (transition is PushDownTransition) {
                        // POP
                        if (transition.pop.isNotEmpty()) {
                            val expectedTop = transition.pop.first()
                            if (newStack.isEmpty() || newStack.last() != expectedTop) continue
                            newStack.removeLast()
                        }

                        // PUSH
                        if (transition.pull.isNotEmpty()) {
                            newStack.add(transition.pull.first())
                        }
                    }

                    nextPaths.add(
                        Path(
                            currentState = nextState,
                            inputIndex = path.inputIndex + 1,
                            symbolStack = newStack
                        )
                    )
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
        builder.appendLine("    <type>$machineType</type>")
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


            if (transition is PushDownTransition) {
                builder.appendLine("            <pop>${transition.pop}</pop>")
                builder.appendLine("            <push>${transition.pull}</push>")
            } else {

                builder.appendLine("            <pop/>")
                builder.appendLine("            <push/>")
            }

            builder.appendLine("        </transition>")
        }

        builder.appendLine("    </automaton>")
        builder.appendLine("</structure>")

        return builder.toString()
    }


    private fun getListOfAppropriateTransitions(startState: State): List<Transition> {
        return transitions.filter { transition ->
            transition.startState == startState.index &&
                    input.startsWith(transition.name) &&
                    (transition !is PushDownTransition || transition.pop.isEmpty() ||
                            (symbolStack.isNotEmpty() && symbolStack.last() == transition.pop.first()))
        }
    }
}


@Composable
fun BottomPushDownBar(pushDownMachine: PushDownMachine) {
    Box(modifier = Modifier.fillMaxSize()){
        LazyRow(
            modifier = Modifier
                .border(
                    3.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = MaterialTheme.shapes.medium
                )
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .height(80.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(pushDownMachine.symbolStack) { symbol ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(MaterialTheme.shapes.large)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.shapes.large
                        )
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = symbol.toString(), fontSize = 30.sp, color = MaterialTheme.colorScheme.tertiary)
                }
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}

class PushDownTransition(
    name: String,
    startState: Int,
    endState: Int,
    var pop: String,
    var pull: String
) : Transition(name, startState, endState)