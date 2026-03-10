package com.sfag.main.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.sfag.main.config.Symbols
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.InputStream
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

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

    /** Parses XML from input stream and returns document. */
    fun parseXml(inputStream: InputStream): Document {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return docBuilder.parse(inputStream)
    }

    /** Gets the JFF type from document (fa, pda, turing, grammar). */
    fun getJffType(doc: Document): String {
        val typeElement = doc.documentElement.getElementsByTagName("type").item(0)
        return typeElement?.textContent?.trim()?.lowercase() ?: "fa"
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
            "epsilon",
            -> ""
            else -> trimmed
        }
    }

    /** Gets text content of a child element. */
    fun Element.getChildText(tagName: String): String? = getElementsByTagName(tagName).item(0)?.textContent

    /** Checks if a child element exists. */
    fun Element.hasChild(tagName: String): Boolean = getElementsByTagName(tagName).length > 0

    /** Escapes XML special characters in text content and attributes. */
    fun escapeXml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    /**
     * Emits a self-closing XML tag for empty values, or a full tag with escaped content otherwise.
     * e.g. xmlTag("read", "") -> "<read/>", xmlTag("read", "a") -> "<read>a</read>"
     */
    fun xmlTag(
        tag: String,
        value: String,
    ): String = if (value.isEmpty()) "<$tag/>" else "<$tag>${escapeXml(value)}</$tag>"

    /**
     * Formats a float value with 2 decimal places for consistent XML output. Uses Locale.US to
     * ensure dot as decimal separator (required for XML/JFF format).
     */
    fun formatFloat(value: Float): String = "%.2f".format(Locale.US, value)

    /**
     * Builds a complete JFF XML document with the standard header and structure wrapper. [type] is
     * the JFF type tag (fa, pda, turing, grammar). [body] appends the type-specific XML content.
     */
    fun jffDocument(
        type: String,
        body: StringBuilder.() -> Unit,
    ): String =
        buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>""")
            appendLine("<structure>")
            appendLine("    <type>$type</type>")
            body()
            appendLine("</structure>")
        }
}
