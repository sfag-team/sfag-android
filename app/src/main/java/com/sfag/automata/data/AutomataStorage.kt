package com.sfag.automata.data

import android.content.Context
import android.util.Log
import com.sfag.automata.domain.machine.Machine
import com.sfag.main.config.INITIAL_ZOOM
import com.sfag.main.data.Point2D
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

private const val NAME_TAG = "__name:"
private const val INPUT_TAG = "__input:"
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
class AutomataStorage @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val storageDir =
        File(context.filesDir, "automata").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

    private val jffFile = File(storageDir, "__current.jff")
    private val metaFile = File(storageDir, "__current.meta")

    /** Writes pre-built JFF and metadata strings to disk. Call from IO thread. */
    internal fun saveRaw(jffContent: String, metadata: String): Boolean =
        try {
            jffFile.writeText(jffContent)
            metaFile.writeText(metadata)
            true
        } catch (e: Exception) {
            Log.e("AutomataStorage", "Failed to save machine", e)
            false
        }

    /** Builds the metadata string for a machine. Safe to call from main thread. */
    internal fun buildMetadata(
        machine: Machine,
        offsetX: Float,
        offsetY: Float,
        scale: Float,
        dirty: Boolean,
    ): String = buildString {
        appendLine("$NAME_TAG${machine.name.replace('\n', ' ')}")
        machine.savedInputs.forEach { appendLine("$INPUT_TAG$it") }
        appendLine("$CANVAS_TAG$offsetX,$offsetY,$scale")
        appendLine("$DIRTY_TAG$dirty")
    }

    fun hasStoredMachine(): Boolean = jffFile.exists()

    internal fun loadMachine(): StoredMachine? {
        if (!jffFile.exists()) {
            return null
        }

        return try {
            val jff = jffFile.inputStream().use { Jff.parse(it) }
            val metaLines = if (metaFile.exists()) metaFile.readLines() else emptyList()

            val canvasParts =
                metaLines
                    .lastOrNull { it.startsWith(CANVAS_TAG) }
                    ?.removePrefix(CANVAS_TAG)
                    ?.split(",")
            val offsetX = canvasParts?.getOrNull(0)?.toFloatOrNull() ?: 0f
            val offsetY = canvasParts?.getOrNull(1)?.toFloatOrNull() ?: 0f
            val scale = canvasParts?.getOrNull(2)?.toFloatOrNull() ?: INITIAL_ZOOM

            val dirtyLine = metaLines.lastOrNull { it.startsWith(DIRTY_TAG) }
            val dirty = dirtyLine?.removePrefix(DIRTY_TAG)?.toBooleanStrictOrNull() ?: false

            val nameLine = metaLines.firstOrNull { it.startsWith(NAME_TAG) }
            val name = nameLine?.removePrefix(NAME_TAG) ?: metaLines.firstOrNull() ?: ""
            val savedInputs =
                metaLines
                    .filter { it.startsWith(INPUT_TAG) }
                    .map { StringBuilder(it.removePrefix(INPUT_TAG)) }
                    .toMutableList()

            StoredMachine(
                jff.toMachine(name, savedInputs),
                jff.positions,
                offsetX,
                offsetY,
                scale,
                dirty,
            )
        } catch (e: Exception) {
            Log.e("AutomataStorage", "Failed to load machine", e)
            null
        }
    }
}
