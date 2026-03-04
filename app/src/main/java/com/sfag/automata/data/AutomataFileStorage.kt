package com.sfag.automata.data

import android.content.Context
import com.sfag.automata.domain.model.Vec2
import com.sfag.automata.domain.model.machine.FiniteMachine
import com.sfag.automata.domain.model.machine.Machine
import com.sfag.automata.domain.model.machine.MachineType
import com.sfag.automata.domain.model.machine.PushDownMachine
import com.sfag.automata.domain.model.machine.TuringMachine
import com.sfag.automata.domain.model.transition.PushDownTransition
import com.sfag.automata.domain.model.transition.TuringTransition
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * File-based storage for automata machines using .jff files
 */
internal class AutomataFileStorage @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val storageDir: File
        get() {
            val dir = File(context.filesDir, "automata")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Auto-save the current machine to a fixed internal file.
     * JFF goes to __current.jff; name + savedInputs go to __current.meta.
     */
    fun saveCurrentMachine(machine: Machine, positions: Map<Int, Vec2>) {
        File(storageDir, "__current.jff").writeText(machine.exportToJFF(positions))
        val meta = buildString {
            appendLine(machine.name)
            machine.savedInputs.forEach { appendLine(it.toString()) }
        }
        File(storageDir, "__current.meta").writeText(meta)
    }

    /**
     * Load the auto-saved current machine.
     * Returns the machine and its positions, or null if no saved machine exists.
     */
    fun loadCurrentMachine(): Pair<Machine, Map<Int, Vec2>>? {
        val jffFile = File(storageDir, "__current.jff")
        if (!jffFile.exists()) return null

        val parseResult = JffParser.parseJffWithType(jffFile.readText())
        val metaFile = File(storageDir, "__current.meta")
        val lines = if (metaFile.exists()) metaFile.readLines() else emptyList()
        val name = lines.firstOrNull() ?: "untitled"
        val savedInputs = lines.drop(1)
            .filter { it.isNotEmpty() }
            .map { StringBuilder(it) }
            .toMutableList()

        val machine = when (parseResult.machineType) {
            MachineType.Finite -> FiniteMachine(
                name = name,
                version = 1,
                states = parseResult.states.toMutableList(),
                transitions = parseResult.transitions.toMutableList(),
                savedInputs = savedInputs
            )
            MachineType.Pushdown -> PushDownMachine(
                name = name,
                version = 1,
                states = parseResult.states.toMutableList(),
                transitions = parseResult.transitions.filterIsInstance<PushDownTransition>().toMutableList(),
                savedInputs = savedInputs
            )
            MachineType.Turing -> TuringMachine(
                name = name,
                version = 1,
                states = parseResult.states.toMutableList(),
                transitions = parseResult.transitions.filterIsInstance<TuringTransition>().toMutableList(),
                savedInputs = savedInputs
            )
        }
        return machine to parseResult.positions
    }

}
