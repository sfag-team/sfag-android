package com.sfag.automata.data

import android.util.Log
import org.w3c.dom.Element
import org.w3c.dom.Node

import com.sfag.automata.model.machine.FiniteMachine
import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.machine.MachineType
import com.sfag.automata.model.machine.PushdownMachine
import com.sfag.automata.model.machine.State
import com.sfag.automata.model.machine.TuringMachine
import com.sfag.automata.model.machine.Vec2
import com.sfag.automata.model.transition.PushdownTransition
import com.sfag.automata.model.transition.TapeDirection
import com.sfag.automata.model.transition.Transition
import com.sfag.automata.model.transition.TuringTransition
import com.sfag.shared.Symbols
import com.sfag.shared.util.JffUtils
import com.sfag.shared.util.JffUtils.getChildText
import com.sfag.shared.util.JffUtils.hasChild

/**
 * Result of parsing a JFF file.
 */
data class JffParseResult(
    val machineType: MachineType,
    val states: List<State>,
    val transitions: List<Transition>,
    val positions: Map<Int, Vec2>
)

fun JffParseResult.toMachine(
    name: String,
    savedInputs: MutableList<StringBuilder> = mutableListOf()
): Machine = when (machineType) {
    MachineType.Finite -> FiniteMachine(
        name = name,
        states = states.toMutableList(),
        transitions = transitions.toMutableList(),
        savedInputs = savedInputs
    )
    MachineType.Pushdown -> PushdownMachine(
        name = name,
        states = states.toMutableList(),
        transitions = transitions.filterIsInstance<PushdownTransition>().toMutableList(),
        savedInputs = savedInputs
    )
    MachineType.Turing -> TuringMachine(
        name = name,
        states = states.toMutableList(),
        transitions = transitions.filterIsInstance<TuringTransition>().toMutableList(),
        savedInputs = savedInputs
    )
}

/**
 * Automata-specific JFF file operations.
 * Uses shared JffUtils for common operations.
 */
object JffParser {
    private const val TAG = "JffParser"

    /**
     * Parses a JFF XML string and returns machine type, states, transitions, and state positions.
     */
    fun parseJffWithType(jffXml: String): JffParseResult {
        val states = mutableListOf<State>()
        val transitions = mutableListOf<Transition>()
        val positions = mutableMapOf<Int, Vec2>()

        val doc = JffUtils.parseXml(jffXml)
        val root = doc.documentElement

        val machineTypeTag = JffUtils.getJffType(doc)
        val machineType = MachineType.fromTag(machineTypeTag)
        val isPda = machineType == MachineType.Pushdown
        val isTuring = machineType == MachineType.Turing

        val automaton = root.getElementsByTagName("automaton").item(0)
        if (automaton == null) {
            Log.w(TAG, "No automaton element found in JFF file")
            return JffParseResult(machineType, states, transitions, positions)
        }

        val nodeList = automaton.childNodes

        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val element = node as Element

            when (element.tagName) {
                "state" -> parseState(element)?.let { (state, pos) ->
                    states.add(state)
                    positions[state.index] = pos
                }
                "transition" -> parseTransition(element, isPda, isTuring)?.let { transitions.add(it) }
            }
        }

        return JffParseResult(machineType, states, transitions, positions)
    }

    private fun parseState(element: Element): Pair<State, Vec2>? {
        val id = element.getAttribute("id").toIntOrNull()
        if (id == null) {
            Log.w(TAG, "Invalid state ID: ${element.getAttribute("id")}")
            return null
        }

        val name = element.getAttribute("name").ifEmpty { "q$id" }
        val x = element.getChildText("x")?.toFloatOrNull() ?: 0f
        val y = element.getChildText("y")?.toFloatOrNull() ?: 0f
        val isInitial = element.hasChild("initial")
        val isFinal = element.hasChild("final")

        val state = State(
            final = isFinal,
            initial = isInitial,
            index = id,
            name = name,
            isCurrent = false
        )
        return state to Vec2(x, y)
    }

    private fun parseTransition(element: Element, isPda: Boolean, isTuring: Boolean): Transition? {
        val from = element.getChildText("from")?.toIntOrNull()
        val to = element.getChildText("to")?.toIntOrNull()

        if (from == null || to == null) {
            Log.w(TAG, "Invalid transition: from=$from, to=$to")
            return null
        }

        val read = JffUtils.normalizeEpsilon(element.getChildText("read") ?: "")

        return when {
            isPda -> {
                val pop = JffUtils.normalizeEpsilon(element.getChildText("pop") ?: "")
                val push = JffUtils.normalizeEpsilon(element.getChildText("push") ?: "")
                PushdownTransition(name = read, startState = from, endState = to, pop = pop, push = push)
            }
            isTuring -> {
                val writeText = element.getChildText("write")?.trim()
                val write = if (writeText.isNullOrEmpty()) Symbols.BLANK else writeText.first()
                val move = element.getChildText("move")?.trim() ?: "R"
                TuringTransition(
                    name = read, startState = from, endState = to,
                    writeSymbol = write, direction = TapeDirection.fromSymbol(move)
                )
            }
            else -> Transition(read, from, to)
        }
    }
}
