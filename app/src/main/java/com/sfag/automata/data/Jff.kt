package com.sfag.automata.data

import android.util.Log
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.FiniteTransition
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.PushdownTransition
import com.sfag.automata.domain.machine.State
import com.sfag.automata.domain.machine.TapeDirection
import com.sfag.automata.domain.machine.Transition
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.automata.domain.machine.TuringTransition
import com.sfag.main.config.Symbols
import com.sfag.main.data.JffUtils
import com.sfag.main.data.JffUtils.getChildText
import com.sfag.main.data.JffUtils.hasChild
import com.sfag.main.data.Point2D
import org.w3c.dom.Element
import java.io.InputStream

/** Parsed contents of a .jff file - states, transitions, and positions. */
internal data class Jff(
    val jffTag: String,
    val states: List<State>,
    val transitions: List<Transition>,
    val positions: Map<Int, Point2D>,
) {
    companion object {
        private const val TAG = "Jff"

        /** Parses a JFF XML stream and returns states, transitions, and state positions. */
        fun parse(inputStream: InputStream): Jff {
            val states = mutableListOf<State>()
            val transitions = mutableListOf<Transition>()
            val positions = mutableMapOf<Int, Point2D>()

            val doc = JffUtils.parseXml(inputStream)
            val root = doc.documentElement

            val jffTag = JffUtils.getJffType(doc)

            val automaton = root.getElementsByTagName("automaton").item(0)
            if (automaton == null) {
                Log.w(TAG, "No automaton element found in JFF file")
                return Jff(jffTag, states, transitions, positions)
            }

            val nodeList = automaton.childNodes

            for (i in 0 until nodeList.length) {
                val element = nodeList.item(i) as? Element ?: continue

                when (element.tagName) {
                    "state" ->
                        parseState(element)?.let { (state, position) ->
                            states.add(state)
                            positions[state.index] = position
                        }
                    "transition" ->
                        parseTransition(element, jffTag)?.let { transitions.add(it) }
                }
            }

            return Jff(jffTag, states, transitions, positions)
        }

        private fun parseState(element: Element): Pair<State, Point2D>? {
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

            val state =
                State(
                    final = isFinal,
                    initial = isInitial,
                    index = id,
                    name = name,
                    isCurrent = false,
                )
            return state to Point2D(x, y)
        }

        private fun parseTransition(
            element: Element,
            jffTag: String,
        ): Transition? {
            val fromState = element.getChildText("from")?.toIntOrNull()
            val toState = element.getChildText("to")?.toIntOrNull()

            if (fromState == null || toState == null) {
                Log.w(TAG, "Invalid transition: from=$fromState, to=$toState")
                return null
            }

            val readSymbol = JffUtils.normalizeEpsilon(element.getChildText("read") ?: "")

            return when (jffTag) {
                "fa" -> FiniteTransition(readSymbol, fromState, toState)
                "pda" -> {
                    val pop = JffUtils.normalizeEpsilon(element.getChildText("pop") ?: "")
                    val push = JffUtils.normalizeEpsilon(element.getChildText("push") ?: "")
                    PushdownTransition(
                        name = readSymbol,
                        fromState = fromState,
                        toState = toState,
                        pop = pop,
                        push = push,
                    )
                }
                "turing" -> {
                    val readText = element.getChildText("read")?.trim()
                    val turingRead =
                        if (readText.isNullOrEmpty()) Symbols.BLANK_CHAR.toString() else readText
                    val writeText = element.getChildText("write")?.trim()
                    val writeSymbol =
                        if (writeText.isNullOrEmpty()) Symbols.BLANK_CHAR else writeText.first()
                    val directionSymbol = element.getChildText("move")?.trim() ?: "R"
                    TuringTransition(
                        name = turingRead,
                        fromState = fromState,
                        toState = toState,
                        writeSymbol = writeSymbol,
                        direction = TapeDirection.fromSymbol(directionSymbol),
                    )
                }
                else -> {
                    Log.w(TAG, "Unknown JFF type: $jffTag")
                    null
                }
            }
        }
    }
}

internal fun Jff.toMachine(
    name: String,
    savedInputs: MutableList<StringBuilder> = mutableListOf(),
): Machine =
    when (jffTag) {
        "fa" ->
            FiniteMachine(
                name = name,
                states = states.toMutableList(),
                transitions = transitions.filterIsInstance<FiniteTransition>().toMutableList(),
                savedInputs = savedInputs,
            )
        "pda" ->
            PushdownMachine(
                name = name,
                states = states.toMutableList(),
                pdaTransitions =
                    transitions.filterIsInstance<PushdownTransition>().toMutableList(),
                savedInputs = savedInputs,
            )
        "turing" ->
            TuringMachine(
                name = name,
                states = states.toMutableList(),
                turingTransitions =
                    transitions.filterIsInstance<TuringTransition>().toMutableList(),
                savedInputs = savedInputs,
            )
        else -> throw IllegalArgumentException("Unknown JFF type: $jffTag")
    }
