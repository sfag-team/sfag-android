package com.sfag.automata.data

import android.content.Context
import com.sfag.automata.domain.machine.Machine
import com.sfag.main.data.Point2D
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** File-based storage for automata using .jff files */
internal class Storage
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
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
         * Auto-save the current machine to a fixed internal file. JFF goes to __current.jff; name +
         * savedInputs go to __current.meta.
         */
        fun saveMachine(
            machine: Machine,
            positions: Map<Int, Point2D>,
        ) {
            File(storageDir, "__current.jff").writeText(machine.exportToJff(positions))
            val metadata =
                buildString {
                    appendLine(machine.name)
                    machine.savedInputs.forEach { appendLine(it.toString()) }
                }
            File(storageDir, "__current.meta").writeText(metadata)
        }

        /**
         * Load the auto-saved current machine. Returns the machine and its positions, or null if no
         * saved machine exists.
         */
        fun loadMachine(): Pair<Machine, Map<Int, Point2D>>? {
            val jffFile = File(storageDir, "__current.jff")
            if (!jffFile.exists()) return null

            val jff = jffFile.inputStream().use { Jff.parse(it) }
            val metaFile = File(storageDir, "__current.meta")
            val lines = if (metaFile.exists()) metaFile.readLines() else emptyList()
            val name = lines.firstOrNull() ?: "untitled"
            val savedInputs =
                lines
                    .drop(1)
                    .filter { it.isNotEmpty() }
                    .map { StringBuilder(it) }
                    .toMutableList()

            val machine = jff.toMachine(name, savedInputs)
            return machine to jff.positions
        }
    }
