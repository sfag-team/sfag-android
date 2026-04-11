package com.sfag.main.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.sfag.main.config.Symbols
import java.io.File
import org.w3c.dom.Document

/** Utilities for JFF (JFLAP) file operations. */
object JffUtils {
    /** Shares JFF file via Android share sheet. */
    fun shareFile(
        context: Context,
        jffContent: String,
        filename: String,
        shareMessage: String = "Share file",
    ) {
        val file = File(context.cacheDir, "$filename.jff")
        file.writeText(jffContent)

        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        context.startActivity(Intent.createChooser(shareIntent, shareMessage))
    }

    /** Gets the JFF type from document (fa, pda, turing, grammar). Throws on missing type. */
    fun getJffType(doc: Document): String {
        val typeElement =
            doc.documentElement.getElementsByTagName("type").item(0)
                ?: throw IllegalArgumentException("Invalid JFF file: missing <type> element")
        return typeElement.textContent.trim().lowercase()
    }

    /**
     * Normalizes epsilon (ε) symbols to empty string. JFLAP uses various representations: empty
     * string, ε, λ, "eps", "epsilon"
     */
    fun normalizeEpsilon(value: String): String {
        val trimmed = value.trim()
        return when (trimmed.lowercase()) {
            "",
            Symbols.EPSILON,
            Symbols.LAMBDA,
            "eps",
            "epsilon" -> ""

            else -> trimmed
        }
    }

    /**
     * Builds a complete JFF XML document with the standard header and structure wrapper. [type] is
     * the JFF type tag (fa, pda, turing, grammar). [body] appends the type-specific XML content.
     */
    fun jffDocument(type: String, body: StringBuilder.() -> Unit): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>""")
        appendLine("<structure>")
        appendLine("    <type>$type</type>")
        body()
        appendLine("</structure>")
    }
}
