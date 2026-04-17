package com.sfag.main.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.sfag.main.config.Symbols
import java.io.File
import org.w3c.dom.Document

/** Utilities for JFF (JFLAP) file operations. */
object JffUtils {
    /** Appends `.jff` unless [name] already ends with it (case-insensitive). */
    fun addJffExtension(name: String): String =
        if (name.endsWith(".jff", ignoreCase = true)) name else "$name.jff"

    /** Strips a trailing `.jff` extension if present (case-insensitive). */
    fun stripJffExtension(name: String): String =
        if (name.endsWith(".jff", ignoreCase = true)) name.dropLast(4) else name

    /** Reads [uri]'s display name and strips the `.jff` extension; returns "" if unavailable. */
    fun getJffStem(context: Context, uri: Uri): String {
        val displayName =
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: ""
        return stripJffExtension(displayName)
    }

    /** Shares JFF file via Android share sheet. */
    fun shareFile(
        context: Context,
        name: String,
        jffContent: String,
        shareMessage: String = "Share file",
    ) {
        val file = File(context.cacheDir, addJffExtension(name))
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
     * Collapses known epsilon aliases to empty string. Aliases: empty, ε, λ, "eps", "epsilon"
     * (case-insensitive). Any other value is returned unchanged. Caller is responsible for trim.
     */
    fun normalizeEpsilon(value: String): String =
        when (value.lowercase()) {
            "",
            Symbols.EPSILON,
            Symbols.LAMBDA,
            "eps",
            "epsilon" -> ""

            else -> value
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
