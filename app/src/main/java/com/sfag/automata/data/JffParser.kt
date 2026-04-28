package com.sfag.automata.data

import android.util.Log
import com.sfag.automata.domain.machine.FaTransition
import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PdaTransition
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.State
import com.sfag.automata.domain.machine.TapeDirection
import com.sfag.automata.domain.machine.TmTransition
import com.sfag.automata.domain.machine.Transition
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.main.config.Symbols
import com.sfag.main.data.JffUtils
import com.sfag.main.data.Point2D
import com.sfag.main.data.XmlUtils
import com.sfag.main.data.XmlUtils.getChildText
import com.sfag.main.data.XmlUtils.hasChild
import java.io.InputStream
import org.w3c.dom.Element

/** Parsed contents of a .jff file - states, transitions, and positions. */
internal data class Jff(
    val jffTag: String,
    val states: List<State>,
    val transitions: List<Transition>,
    val positions: Map<Int, Point2D>,
) {
    companion object {
        /** Parses a JFF XML stream and returns states, transitions, and state positions. */
        fun parse(inputStream: InputStream): Jff {
            val states = mutableListOf<State>()
            val transitions = mutableListOf<Transition>()
            val positions = mutableMapOf<Int, Point2D>()

            val doc = XmlUtils.parseXml(inputStream)
            val root = doc.documentElement

            val jffTag = JffUtils.getJffType(doc)

            val automaton = root.getElementsByTagName("automaton").item(0)
            if (automaton == null) {
                Log.e("JffParser", "No automaton element found in JFF file")
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

                    "transition" -> parseTransition(element, jffTag)?.let { transitions.add(it) }
                }
            }

            return Jff(jffTag, states, transitions, positions)
        }

        private fun parseState(element: Element): Pair<State, Point2D>? {
            val id = element.getAttribute("id").toIntOrNull()
            if (id == null) {
                Log.w("JffParser", "Invalid state ID: ${element.getAttribute("id")}")
                return null
            }

            val name = element.getAttribute("name").ifEmpty { "q$id" }
            val x = element.getChildText("x")?.toFloatOrNull() ?: 0f
            val y = element.getChildText("y")?.toFloatOrNull() ?: 0f
            val isInitial = element.hasChild("initial")
            val isFinal = element.hasChild("final")

            val state = State(final = isFinal, initial = isInitial, index = id, name = name)
            return state to Point2D(x, y)
        }

        private fun parseTransition(element: Element, jffTag: String): Transition? {
            val fromState = element.getChildText("from")?.toIntOrNull()
            val toState = element.getChildText("to")?.toIntOrNull()

            if (fromState == null || toState == null) {
                Log.w("JffParser", "Invalid transition: from=$fromState, to=$toState")
                return null
            }

            val readSymbol = JffUtils.normalizeEpsilon(element.getChildText("read")?.trim() ?: "")

            return when (jffTag) {
                "fa" -> FaTransition(fromState, toState, readSymbol)
                "pda" -> {
                    val pop = JffUtils.normalizeEpsilon(element.getChildText("pop")?.trim() ?: "")
                    val push = JffUtils.normalizeEpsilon(element.getChildText("push")?.trim() ?: "")
                    PdaTransition(
                        fromState = fromState,
                        toState = toState,
                        read = readSymbol,
                        pop = pop,
                        push = push,
                    )
                }

                "turing" -> {
                    val readText = element.getChildText("read")?.trim()
                    val turingRead =
                        if (readText.isNullOrEmpty()) Symbols.BLANK_CHAR.toString() else readText
                    val writeText = element.getChildText("write")?.trim()
                    val write =
                        if (writeText.isNullOrEmpty()) Symbols.BLANK_CHAR else writeText.first()
                    val directionSymbol = element.getChildText("move")?.trim() ?: "R"
                    TmTransition(
                        fromState = fromState,
                        toState = toState,
                        read = turingRead,
                        write = write,
                        direction = TapeDirection.fromSymbol(directionSymbol),
                    )
                }

                else -> {
                    Log.e("JffParser", "Unknown JFF type: $jffTag")
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
                transitions = transitions.filterIsInstance<FaTransition>().toMutableList(),
                savedInputs = savedInputs,
            )

        "pda" ->
            PushdownMachine(
                name = name,
                states = states.toMutableList(),
                pdaTransitions = transitions.filterIsInstance<PdaTransition>().toMutableList(),
                savedInputs = savedInputs,
            )

        "turing" ->
            TuringMachine(
                name = name,
                states = states.toMutableList(),
                tmTransitions = transitions.filterIsInstance<TmTransition>().toMutableList(),
                savedInputs = savedInputs,
            )

        else -> throw IllegalArgumentException("Unknown JFF type: $jffTag")
    }
