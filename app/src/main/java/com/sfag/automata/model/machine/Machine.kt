package com.sfag.automata.model.machine

import com.sfag.automata.model.simulation.ExplorationTree
import com.sfag.automata.model.simulation.SimulationStep

import com.sfag.automata.model.transition.Transition
import com.sfag.shared.util.XmlUtils

data class Vec2(val x: Float = 0f, val y: Float = 0f)

abstract class Machine(
    var name: String,
    var version: Int,
    val machineType: MachineType,
    val states: MutableList<State>,
    val transitions: MutableList<Transition>,
    var imuInput: StringBuilder = java.lang.StringBuilder(),
    val savedInputs: MutableList<StringBuilder>
) {
    // Snapshot of full input for simulation reset
    var fullInputSnapshot: String = ""

    var input = StringBuilder()
    val derivationTree = ExplorationTree()
    abstract var currentState: Int?

    /**
     * delete transition
     */
    fun deleteTransition(transition: Transition) {
        transitions.remove(transition)
    }

    /**
     * delete state and all transitions that reference it
     */
    fun deleteState(state: State) {
        states.remove(state)
        transitions.filter { it.startState == state.index || it.endState == state.index }
            .forEach { transitionToRemove ->
                transitions.remove(transitionToRemove)
            }
    }

    /**
     * Calculates the next simulation step.
     * Returns a SimulationStep that the UI layer uses to display animation.
     */
    abstract fun calculateNextStep(): SimulationStep

    /**
     * Returns state with the given index, or throws if not found.
     */
    fun getStateByIndex(index: Int): State = states.firstOrNull { it.index == index }
        ?: throw IllegalArgumentException("State with index $index not found")

    /**
     * Returns state with the given index, or null if not found.
     */
    fun getStateByIndexOrNull(index: Int): State? = states.firstOrNull { it.index == index }

    fun setInitialStateAsCurrent() {
        currentState?.let { stateIndex ->
            getStateByIndexOrNull(stateIndex)?.isCurrent = false
        }
        derivationTree.clear()
        states.firstOrNull { it.initial }?.let { initialState ->
            initialState.isCurrent = true
            currentState = initialState.index
            derivationTree.initialize(initialState.name)
        }
        resetMachineState()
    }

    abstract fun addNewState(state: State)

    /**
     * Adds a transition, merging name if an identical one already exists.
     */
    fun addNewTransition(name: String, startState: State, endState: State) {
        val alreadyExists = transitions.any { t ->
            t.startState == startState.index && t.endState == endState.index && t.name == name
        }
        if (!alreadyExists) {
            transitions.add(Transition(name, startState.index, endState.index))
        }
    }

    abstract fun expandExplorationTree()

    /**
     * Returns the formal mathematical definition data for this machine.
     */
    abstract fun getFormalDefinition(): MachineFormalDefinition

    abstract fun canReachFinalState(input: StringBuilder, fromInit: Boolean): Boolean

    /**
     * Exports machine to JFLAP-compatible JFF format.
     * @param positions state index → Vec2 position in dp
     */
    fun exportToJFF(positions: Map<Int, Vec2>): String {
        val builder = StringBuilder()
        builder.appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>""")
        builder.appendLine("<structure>")
        builder.appendLine("    <type>${machineType.tag}</type>")
        builder.appendLine("    <automaton>")

        for (state in states) {
            val escapedName = XmlUtils.escapeXml(state.name)
            val pos = positions[state.index] ?: Vec2()
            builder.appendLine("""        <state id="${state.index}" name="$escapedName">""")
            builder.appendLine("""            <x>${XmlUtils.formatFloat(pos.x)}</x>""")
            builder.appendLine("""            <y>${XmlUtils.formatFloat(pos.y)}</y>""")
            if (state.initial) builder.appendLine("            <initial/>")
            if (state.final) builder.appendLine("            <final/>")
            builder.appendLine("        </state>")
        }

        exportTransitionsToJFF(builder)

        builder.appendLine("    </automaton>")
        builder.appendLine("</structure>")

        return builder.toString()
    }

    /**
     * Exports transitions to JFF format. Override in subclasses for machine-specific format.
     */
    protected abstract fun exportTransitionsToJFF(builder: StringBuilder)

    /**
     * Called when resetting simulation to initial state.
     */
    open fun resetMachineState() {}

    protected fun ensureCurrentState(): Boolean {
        if (currentState == null) currentState = states.firstOrNull { it.initial }?.index
        return currentState != null
    }
}
