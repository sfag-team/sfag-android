package com.droidhen.formalautosim.core.entities.machines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    val symbolStack: MutableList<Char> = mutableListOf('Z')
) : Machine(
    name, version,
    machineType = MachineType.Pushdown, states, transitions as MutableList<Transition>, savedInputs = savedInputs
) {
    override var currentState: Int? = null
    var acceptanceCriteria = AcceptanceCriteria.BY_FINITE_STATE



    @Composable
    override fun calculateTransition(onAnimationEnd: (Boolean?) -> Unit) {
        if (currentState == null) currentState = states.firstOrNull { it.initial }?.index
        if (currentState == null) {
            onAnimationEnd(null)
            return
        }

        val startState = getStateByIndex(currentState!!)
        val conditionDone = if(acceptanceCriteria == AcceptanceCriteria.BY_FINITE_STATE) startState.finite else symbolStack.size==1
        val possibleTransitions = getListOfAppropriateTransitions(startState)
        if (possibleTransitions.isEmpty()) {
            onAnimationEnd(conditionDone)
            return
        }

        var validTransition: Transition? = null

        validTransition = possibleTransitions.firstOrNull { transition ->
            val nextInput = input.removePrefix(transition.name)
            val tempStack = symbolStack.toMutableList()

            if (transition is PushDownTransition) {
                // POP
                if (transition.pop.isNotEmpty()) {
                    val expectedTop = transition.pop.first()
                    if (tempStack.isEmpty() || tempStack.last() != expectedTop) return@firstOrNull false
                    tempStack.removeLast()
                }

                // PUSH (в правильном порядке: последний символ — верх стека)
                if (transition.push.isNotEmpty()) {
                    transition.push.reversed().forEach { symbol ->
                        tempStack.add(symbol)
                    }
                }
            }

            // check if we can reach final state
            val nextState = getStateByIndex(transition.endState)
            val previousCurrent = currentState

            states.forEach { it.isCurrent = false }
            nextState.isCurrent = true
            currentState = nextState.index

            val result = when (acceptanceCriteria) {
                AcceptanceCriteria.BY_FINITE_STATE -> canReachFinalStatePDA(StringBuilder(nextInput),false,  tempStack)
                AcceptanceCriteria.BY_INITIAL_STACK -> canReachInitialStackPDA(StringBuilder(nextInput),false, tempStack)
            }


            nextState.isCurrent = false
            getStateByIndex(previousCurrent!!).isCurrent = true
            currentState = previousCurrent

            result
        }

        // fallback transition
        if (validTransition == null) {
            validTransition = possibleTransitions.first()
        }

        val endState = getStateByIndex(validTransition.endState)
        val newInputValue = input.removePrefix(validTransition.name).toString()
        input.clear()
        input.append(newInputValue)

        // реальный пуш/поп в symbolStack
        if (validTransition is PushDownTransition) {
            // POP
            if (validTransition.pop.isNotEmpty()) {
                symbolStack.removeLast()
            }

            // PUSH (в правильном порядке)
            if (validTransition.push.isNotEmpty()) {
                validTransition.push.reversed().forEach { symbol ->
                    symbolStack.add(symbol)
                }
            }
        }

        currentTreePosition++

        AnimationOfTransition(
            start = startState.position,
            end = endState.position,
            radius = startState.radius,
            duration = 500,
            onAnimationEnd = {
                startState.isCurrent = false
                endState.isCurrent = true
                currentState = endState.index
                onAnimationEnd(if(input.isEmpty() && if(acceptanceCriteria == AcceptanceCriteria.BY_FINITE_STATE) {endState.finite} else {symbolStack.size==1})  true else null)
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
        val allPaths = mutableListOf<Pair<List<String?>, List<Char>>>()

        data class Path(
            val history: List<String?>,
            val currentState: State?,
            val inputIndex: Int,
            val symbolStack: List<Char>,
            val alive: Boolean
        )

        val startStates = states.filter { it.initial }
        var paths = startStates.map {
            Path(listOf(null), it, 0, listOf('Z'), true)
        }.toMutableList()

        while (paths.any { it.alive }) {
            val nextPaths = mutableListOf<Path>()

            paths.forEach { path ->
                if (!path.alive) {
                    nextPaths.add(path.copy(history = path.history + null, currentState = null, alive = false))
                    return@forEach
                }

                val currentChar = imuInput.getOrNull(path.inputIndex)
                val currentStack = path.symbolStack.toMutableList()

                val possibleTransitions = transitions
                    .filter { it.startState == path.currentState?.index }
                    .filter { it.name.isEmpty() || it.name.firstOrNull() == currentChar }

                if (possibleTransitions.isEmpty()) {
                    allPaths.add(path.history + path.currentState?.name to path.symbolStack)
                    nextPaths.add(path.copy(history = path.history + null, currentState = null, alive = false))
                    return@forEach
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    val newStack = currentStack.toMutableList()

                    if (transition is PushDownTransition) {
                        if (transition.pop.isNotEmpty()) {
                            val expectedTop = transition.pop.first()
                            if (newStack.isEmpty() || newStack.last() != expectedTop) continue
                            newStack.removeLast()
                        }

                        if (transition.push.isNotEmpty()) {
                            transition.push.reversed().forEach { symbol ->
                                newStack.add(symbol)
                            }
                        }
                    }

                    val newInputIndex = if (transition.name.isEmpty()) path.inputIndex else path.inputIndex + 1

                    nextPaths.add(
                        Path(
                            history = path.history + path.currentState?.name,
                            currentState = nextState,
                            inputIndex = newInputIndex,
                            symbolStack = newStack,
                            alive = true
                        )
                    )
                }
            }

            paths = nextPaths
        }

        val acceptedPaths = allPaths.filter { (path, stack) ->
            val last = path.lastOrNull()
            when (acceptanceCriteria) {
                AcceptanceCriteria.BY_FINITE_STATE ->
                    last != null && states.any { it.name == last && it.finite }

                AcceptanceCriteria.BY_INITIAL_STACK ->
                    stack == listOf('Z')
            }
        }.map { it.first }

        val maxDepth = allPaths.maxOfOrNull { it.first.size } ?: 0
        val normalizedPaths = allPaths.map { (path, _) ->
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

                val isCurrent = stateName != null &&
                        states.firstOrNull { it.name == stateName }?.isCurrent == true &&
                        currentTreePosition == level

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

    @Composable
    override fun MathFormat() {
        val initialState = states.firstOrNull { it.initial }?.name ?: "q₀"
        val finalStates = states.filter { it.finite }.joinToString(", ") { it.name }
        transitions as MutableList<PushDownTransition>
        val inputAlphabet = transitions.mapNotNull { it.name.firstOrNull() }.toSet().joinToString(", ")
        val stackAlphabet = transitions.flatMap { (it.pop + it.push).toCharArray().toList() }.toSet().joinToString(", ")

        val deltaList = transitions.filterIsInstance<PushDownTransition>().joinToString("\n") { t ->
            val fromState = states.find { it.index == t.startState }?.name ?: "?"
            val toState = states.find { it.index == t.endState }?.name ?: "?"
            val readSymbol = if (t.name.isEmpty()) "ε" else t.name
            val popSymbol = if (t.pop.isEmpty()) "ε" else t.pop
            val pushSymbol = if (t.push.isEmpty()) "ε" else t.push
            "δ($fromState, $readSymbol, $popSymbol) = ($toState, $pushSymbol)"
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text("M = (Q, Σ, Γ, δ, $initialState, $symbolStack, F)", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Q = { ${states.joinToString(", ") { it.name }} }")
            Text("Σ = { $inputAlphabet }")
            Text("Γ = { $stackAlphabet }")
            Text("Z = 'Z'")
            Text("F = { $finalStates }")
            Spacer(modifier = Modifier.height(12.dp))
            Text("δ:", fontWeight = FontWeight.Bold)
            Text(deltaList, fontFamily = FontFamily.Monospace)
        }
    }


    override fun canReachFinalState(input: StringBuilder, fromInit: Boolean): Boolean {
        return canReachFinalStatePDA(input,fromInit = fromInit, initialStack = if(fromInit) listOf('Z') else symbolStack)
    }

    private fun canReachFinalStatePDA(
        input: StringBuilder,
        fromInit:Boolean,
        initialStack: List<Char>
    ): Boolean {

        data class Path(
            val currentState: State,
            val inputIndex: Int,
            val symbolStack: List<Char>
        )

        var startState = states.firstOrNull { if(!fromInit) it.isCurrent else it.initial }
        if (startState == null) {
            setInitialStateAsCurrent()
            startState = states.firstOrNull { it.isCurrent }
        }
        if (startState == null) return false

        var paths = mutableListOf(Path(startState, 0, initialStack))

        while (paths.isNotEmpty()) {
            val nextPaths = mutableListOf<Path>()

            for (path in paths) {
                if (path.inputIndex == input.length && path.currentState.finite) {
                    return true
                }

                val currentChar = input.getOrNull(path.inputIndex)

                val possibleTransitions = transitions.filter {
                    it.startState == path.currentState.index &&
                            (
                                    it.name.isEmpty() || (currentChar != null && it.name.firstOrNull() == currentChar)
                                    )
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    val newStack = path.symbolStack.toMutableList()

                    if (transition is PushDownTransition) {
                        if (transition.pop.isNotEmpty()) {
                            val expectedTop = transition.pop.first()
                            if (newStack.isEmpty() || newStack.last() != expectedTop) continue
                            newStack.removeLast()
                        }

                        if (transition.push.isNotEmpty()) {
                            transition.push.reversed().forEach { symbol ->
                                newStack.add(symbol)
                            }
                        }
                    }

                    val newIndex = if (transition.name.isEmpty()) path.inputIndex else path.inputIndex + 1

                    nextPaths.add(
                        Path(
                            currentState = nextState,
                            inputIndex = newIndex,
                            symbolStack = newStack
                        )
                    )
                }
            }

            paths = nextPaths
        }

        return false
    }



    fun canReachInitialStackPDA(input: StringBuilder, fromInit: Boolean = false, symbolStack: List<Char>): Boolean {
        data class Path(
            val currentState: State,
            val inputIndex: Int,
            val symbolStack: List<Char>
        )

        var startState = states.firstOrNull { if(!fromInit) it.isCurrent else it.initial } ?: run {
            setInitialStateAsCurrent()
            states.firstOrNull { it.isCurrent }
        } ?: return false

        var paths = mutableListOf(Path(startState, 0,symbolStack))

        while (paths.isNotEmpty()) {
            val nextPaths = mutableListOf<Path>()

            for (path in paths) {
                if (path.inputIndex == input.length && path.symbolStack == listOf('Z')) {
                    return true
                }

                val currentChar = input.getOrNull(path.inputIndex)

                val possibleTransitions = transitions.filter {
                    it.startState == path.currentState.index &&
                            (it.name.isEmpty() || it.name.firstOrNull() == currentChar)
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    val newStack = path.symbolStack.toMutableList()

                    if (transition is PushDownTransition) {
                        if (transition.pop.isNotEmpty()) {
                            val expectedTop = transition.pop.first()
                            if (newStack.isEmpty() || newStack.last() != expectedTop) continue
                            newStack.removeLast()
                        }
                        if (transition.push.isNotEmpty()) {
                            transition.push.reversed().forEach { symbol ->
                                newStack.add(symbol)
                            }
                        }
                    }

                    val newIndex = if (transition.name.isEmpty()) path.inputIndex else path.inputIndex + 1

                    nextPaths.add(Path(nextState, newIndex, newStack))
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
                builder.appendLine("            <push>${transition.push}</push>")
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

data class PushDownTransition(
    override var name: String,
    override var startState: Int,
    override var endState: Int,
    var pop: String,
    var push: String
) : Transition(name, startState, endState)

enum class AcceptanceCriteria( val text:String){
    BY_FINITE_STATE("the finite state"),
    BY_INITIAL_STACK("the initial stack (\"Z\")")
}

