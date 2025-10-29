package com.sfag.automata.data.local

import android.content.Context
import com.sfag.automata.core.machine.FiniteMachine
import com.sfag.automata.core.machine.Machine
import com.sfag.automata.core.machine.MachineType
import com.sfag.automata.core.machine.PushDownMachine
import com.sfag.automata.core.transition.PushDownTransition
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import com.sfag.shared.data.local.FileStorage


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
     * Save a machine to a .jff file
     */
    fun saveMachine(machine: Machine) {
        val filename = "${sanitizeFilename(machine.name)}.jff"
        val file = File(storageDir, filename)
        val jffContent = machine.exportToJFF()
        file.writeText(jffContent)
    }

    /**
     * Get all saved machine names
     */
    fun getAllMachineNames(): List<String> {
        return storageDir.listFiles()
            ?.filter { it.extension == "jff" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    /**
     * Load a machine by name from .jff file
     */
    fun getMachineByName(name: String): Machine? {
        val filename = "${sanitizeFilename(name)}.jff"
        val file = File(storageDir, filename)

        if (!file.exists()) {
            return null
        }

        val jffContent = file.readText()
        val (states, transitions) = FileStorage.parseJff(jffContent)

        // Determine machine type from JFF content
        val machineType = if (jffContent.contains("<type>fa</type>")) {
            MachineType.Finite
        } else {
            MachineType.Pushdown
        }

        return if (machineType == MachineType.Finite) {
            FiniteMachine(
                name = name,
                version = 1,
                states = states.toMutableList(),
                transitions = transitions.toMutableList(),
                savedInputs = mutableListOf()
            )
        } else {
            PushDownMachine(
                name = name,
                version = 1,
                states = states.toMutableList(),
                transitions = transitions.filterIsInstance<PushDownTransition>().toMutableList(),
                savedInputs = mutableListOf()
            )
        }
    }

    /**
     * Delete a machine file
     */
    fun deleteMachine(name: String): Boolean {
        val filename = "${sanitizeFilename(name)}.jff"
        val file = File(storageDir, filename)
        return file.delete()
    }

    /**
     * Sanitize filename to remove invalid characters
     */
    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
