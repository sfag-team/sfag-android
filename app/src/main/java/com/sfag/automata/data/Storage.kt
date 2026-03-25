package com.sfag.automata.data

import android.content.Context
import android.util.Log
import com.sfag.automata.domain.machine.Machine
import com.sfag.main.config.INITIAL_ZOOM
import com.sfag.main.data.Point2D
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

private const val CANVAS_TAG = "__canvas:"
private const val DIRTY_TAG = "__dirty:"

internal data class StoredMachine(
    val machine: Machine,
    val positions: Map<Int, Point2D>,
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float,
    val dirty: Boolean,
)

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
        dirty: Boolean,
    ): Boolean =
        try {
            File(storageDir, "__current.jff").writeText(machine.exportToJff(positions))
            val metadata =
                buildString {
                    appendLine(machine.name)
                    machine.savedInputs.forEach { appendLine(it.toString()) }
                    appendLine("$CANVAS_TAG$offsetX,$offsetY,$scale")
                    appendLine("$DIRTY_TAG$dirty")
                }
            File(storageDir, "__current.meta").writeText(metadata)
            true
        } catch (e: Exception) {
            Log.e("Storage", "Failed to save machine", e)
            false
        }

    fun loadMachine(): StoredMachine? {
        val jffFile = File(storageDir, "__current.jff")
        if (!jffFile.exists()) return null

        return try {
            val jff = jffFile.inputStream().use { Jff.parse(it) }
            val metaFile = File(storageDir, "__current.meta")
            val lines = if (metaFile.exists()) metaFile.readLines() else emptyList()
            val name = lines.firstOrNull() ?: "untitled"

            val canvasParts = lines.lastOrNull { it.startsWith(CANVAS_TAG) }
                ?.removePrefix(CANVAS_TAG)?.split(",")
            val offsetX = canvasParts?.getOrNull(0)?.toFloatOrNull() ?: 0f
            val offsetY = canvasParts?.getOrNull(1)?.toFloatOrNull() ?: 0f
            val scale = canvasParts?.getOrNull(2)?.toFloatOrNull() ?: INITIAL_ZOOM

            val dirtyLine = lines.lastOrNull { it.startsWith(DIRTY_TAG) }
            val dirty = dirtyLine?.removePrefix(DIRTY_TAG)?.toBooleanStrictOrNull() ?: false

            val savedInputs =
                lines
                    .drop(1)
                    .filter {
                        it.isNotEmpty() && !it.startsWith(CANVAS_TAG) && !it.startsWith(
                            DIRTY_TAG
                        )
                    }
                    .map { StringBuilder(it) }
                    .toMutableList()

            StoredMachine(
                jff.toMachine(name, savedInputs),
                jff.positions,
                offsetX,
                offsetY,
                scale,
                dirty
            )
        } catch (e: Exception) {
            Log.e("Storage", "Failed to load machine", e)
            null
        }
    }
}
