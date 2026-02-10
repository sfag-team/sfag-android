package com.sfag.automata.data

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.state.State
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.TapeDirection
import com.sfag.automata.domain.model.transition.Transition
import com.sfag.automata.domain.model.transition.TuringTransition
import com.sfag.shared.util.JffFileUtils
import com.sfag.shared.util.JffFileUtils.getChildText
import com.sfag.shared.util.JffFileUtils.hasChild
import com.sfag.shared.util.Symbols
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Result of parsing a JFF file, containing machine type, states, and transitions.
 */
data class JffParseResult(
    val machineType: MachineType,
    val states: List<State>,
    val transitions: List<Transition>
)

/**
 * Automata-specific JFF file operations.
 * Uses shared JffFileUtils for common operations.
 */
object JffParser {
    private const val TAG = "JffParser"

    fun shareJffFile(context: Context, jffContent: String, filename: String) {
        JffFileUtils.shareFile(context, jffContent, filename, "Share automata with your friends")
    }

    /**
     * Parses a JFF XML string and returns states and transitions.
     */
    fun parseJff(jffXml: String): Pair<List<State>, List<Transition>> {
        val result = parseJffWithType(jffXml)
        return Pair(result.states, result.transitions)
    }

    /**
     * Parses a JFF XML string and returns machine type, states, and transitions.
     */
    fun parseJffWithType(jffXml: String): JffParseResult {
        val states = mutableListOf<State>()
        val transitions = mutableListOf<Transition>()

        val doc = JffFileUtils.parseXml(jffXml)
        val root = doc.documentElement

        val machineTypeTag = JffFileUtils.getJffType(doc)
        val machineType = MachineType.fromTag(machineTypeTag)
        val isPda = machineType == MachineType.Pushdown
        val isTuring = machineType == MachineType.Turing

        val automaton = root.getElementsByTagName("automaton").item(0)
        if (automaton == null) {
            Log.w(TAG, "No automaton element found in JFF file")
            return JffParseResult(machineType, states, transitions)
        }

        val nodeList = automaton.childNodes

        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val element = node as Element

            when (element.tagName) {
                "state" -> parseState(element)?.let { states.add(it) }
                "transition" -> parseTransition(element, isPda, isTuring)?.let { transitions.add(it) }
            }
        }

        return JffParseResult(machineType, states, transitions)
    }

    private fun parseState(element: Element): State? {
        val id = element.getAttribute("id").toIntOrNull()
        if (id == null) {
            Log.w(TAG, "Invalid state ID: ${element.getAttribute("id")}")
            return null
        }

        val name = element.getAttribute("name").ifEmpty { "q$id" }
        // TODO: JFLAP uses pixel coordinates, our app uses dp - imported positions appear
        //  too spread out on high-density screens. Convert px→dp on import (divide by density).
        val x = element.getChildText("x")?.toFloatOrNull() ?: 0f
        val y = element.getChildText("y")?.toFloatOrNull() ?: 0f
        val isInitial = element.hasChild("initial")
        val isFinal = element.hasChild("final")

        return State(
            finite = isFinal,
            initial = isInitial,
            index = id,
            name = name,
            isCurrent = false,
            position = Offset(x, y)
        )
    }

    private fun parseTransition(element: Element, isPda: Boolean, isTuring: Boolean): Transition? {
        val from = element.getChildText("from")?.toIntOrNull()
        val to = element.getChildText("to")?.toIntOrNull()

        if (from == null || to == null) {
            Log.w(TAG, "Invalid transition: from=$from, to=$to")
            return null
        }

        var read = element.getChildText("read") ?: ""
        read = JffFileUtils.normalizeEpsilon(read)

        val controlPoint = parseControlPoint(element)

        val transition = when {
            isPda -> {
                var pop = element.getChildText("pop") ?: ""
                var push = element.getChildText("push") ?: ""
                pop = JffFileUtils.normalizeEpsilon(pop)
                push = JffFileUtils.normalizeEpsilon(push)

                PushDownTransition(
                    name = read,
                    startState = from,
                    endState = to,
                    pop = pop,
                    push = push
                )
            }
            isTuring -> {
                val writeText = element.getChildText("write")?.trim()
                val write = if (writeText.isNullOrEmpty()) Symbols.BLANK else writeText.first()
                val move = element.getChildText("move")?.trim() ?: "R"

                TuringTransition(
                    name = read,
                    startState = from,
                    endState = to,
                    writeSymbol = write,
                    direction = TapeDirection.fromSymbol(move)
                )
            }
            else -> Transition(read, from, to)
        }
        transition.controlPoint = controlPoint
        return transition
    }

    private fun parseControlPoint(element: Element): Offset? {
        val controlPointElement = element.getElementsByTagName("controlpoint").item(0)
        if (controlPointElement != null && controlPointElement is Element) {
            val cpX = controlPointElement.getChildText("x")?.toFloatOrNull()
            val cpY = controlPointElement.getChildText("y")?.toFloatOrNull()
            if (cpX != null && cpY != null) {
                return Offset(cpX, cpY)
            }
        }
        return null
    }
}
