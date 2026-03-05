package com.sfag.automata.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

import com.sfag.automata.model.machine.Machine
import com.sfag.automata.model.machine.Vec2

/**
 * File-based storage for automata using .jff files
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

        val machine = parseResult.toMachine(name, savedInputs)
        return machine to parseResult.positions
    }
}
