package com.sfag.automata.data

import android.content.Context
import com.sfag.automata.domain.machine.Machine
import com.sfag.main.data.Point2D
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

private const val CANVAS_TAG = "__canvas:"

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

        fun saveMachine(
            machine: Machine,
            positions: Map<Int, Point2D>,
            offsetX: Float,
            offsetY: Float,
            scale: Float,
        ) {
            File(storageDir, "__current.jff").writeText(machine.exportToJff(positions))
            val metadata =
                buildString {
                    appendLine(machine.name)
                    machine.savedInputs.forEach { appendLine(it.toString()) }
                    append("$CANVAS_TAG$offsetX,$offsetY,$scale")
                }
            File(storageDir, "__current.meta").writeText(metadata)
        }

        /**
         * Load the auto-saved current machine. Returns the machine, positions, and canvas offset,
         * or null if no saved machine exists.
         */
        fun loadMachine(): Triple<Machine, Map<Int, Point2D>, Triple<Float, Float, Float>>? {
            val jffFile = File(storageDir, "__current.jff")
            if (!jffFile.exists()) return null

            val jff = jffFile.inputStream().use { Jff.parse(it) }
            val metaFile = File(storageDir, "__current.meta")
            val lines = if (metaFile.exists()) metaFile.readLines() else emptyList()
            val name = lines.firstOrNull() ?: "untitled"

            val canvasLine = lines.lastOrNull { it.startsWith(CANVAS_TAG) }
            val canvas =
                canvasLine?.removePrefix(CANVAS_TAG)?.split(",")?.let { parts ->
                    Triple(
                        parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
                        parts.getOrNull(1)?.toFloatOrNull() ?: 0f,
                        parts.getOrNull(2)?.toFloatOrNull() ?: 0.5f,
                    )
                } ?: Triple(0f, 0f, 0.5f)

            val savedInputs =
                lines
                    .drop(1)
                    .filter { it.isNotEmpty() && !it.startsWith(CANVAS_TAG) }
                    .map { StringBuilder(it) }
                    .toMutableList()

            val machine = jff.toMachine(name, savedInputs)
            return Triple(machine, jff.positions, canvas)
        }
    }
