package com.sfag.automata.data

import com.sfag.automata.domain.machine.FiniteMachine
import com.sfag.automata.domain.machine.Machine
import com.sfag.automata.domain.machine.PushdownMachine
import com.sfag.automata.domain.machine.TuringMachine
import com.sfag.main.config.Symbols
import com.sfag.main.data.JffUtils
import com.sfag.main.data.JffUtils.escapeXml
import com.sfag.main.data.JffUtils.xmlTag
import com.sfag.main.data.Point2D

fun Machine.exportToJff(positions: Map<Int, Point2D>): String =
    JffUtils.jffDocument(jffTag) {
        appendLine("    <automaton>")

        for (state in states) {
            val escapedName = escapeXml(state.name)
            val position = positions[state.index] ?: Point2D()
            appendLine("""        <state id="${state.index}" name="$escapedName">""")
            appendLine("""            <x>${JffUtils.formatFloat(position.x)}</x>""")
            appendLine("""            <y>${JffUtils.formatFloat(position.y)}</y>""")
            if (state.initial) appendLine("            <initial/>")
            if (state.final) appendLine("            <final/>")
            appendLine("        </state>")
        }

        exportTransitionsToJff(this)

        appendLine("    </automaton>")
    }

private fun Machine.exportTransitionsToJff(builder: StringBuilder) =
    when (this) {
        is FiniteMachine -> exportFiniteTransitions(builder)
        is PushdownMachine -> exportPushdownTransitions(builder)
        is TuringMachine -> exportTuringTransitions(builder)
    }

private fun FiniteMachine.exportFiniteTransitions(builder: StringBuilder) {
    for (transition in transitions) {
        builder.appendLine("        <transition>")
        builder.appendLine("            <from>${transition.fromState}</from>")
        builder.appendLine("            <to>${transition.toState}</to>")
        builder.appendLine("            ${xmlTag("read", transition.name)}")
        builder.appendLine("        </transition>")
    }
}

private fun PushdownMachine.exportPushdownTransitions(builder: StringBuilder) {
    for (transition in pdaTransitions) {
        builder.appendLine("        <transition>")
        builder.appendLine("            <from>${transition.fromState}</from>")
        builder.appendLine("            <to>${transition.toState}</to>")
        builder.appendLine("            ${xmlTag("read", transition.name)}")
        builder.appendLine("            ${xmlTag("pop", transition.pop)}")
        builder.appendLine("            ${xmlTag("push", transition.push)}")
        builder.appendLine("        </transition>")
    }
}

private fun TuringMachine.exportTuringTransitions(builder: StringBuilder) {
    for (transition in turingTransitions) {
        builder.appendLine("        <transition>")
        builder.appendLine("            <from>${transition.fromState}</from>")
        builder.appendLine("            <to>${transition.toState}</to>")
        val readExport =
            if (transition.name == Symbols.BLANK_CHAR.toString()) "" else transition.name
        builder.appendLine("            ${xmlTag("read", readExport)}")
        val writeExport =
            if (transition.writeSymbol == Symbols.BLANK_CHAR) {
                ""
            } else {
                transition.writeSymbol.toString()
            }
        builder.appendLine("            ${xmlTag("write", writeExport)}")
        builder.appendLine("            <move>${transition.direction.symbol}</move>")
        builder.appendLine("        </transition>")
    }
}
