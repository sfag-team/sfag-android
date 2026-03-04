package com.sfag.shared.util

import java.util.Locale

/**
 * Utility functions for XML serialization used in JFF export.
 */
object XmlUtils {
    /**
     * Escapes XML special characters in text content and attributes.
     */
    fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    /**
     * Emits a self-closing XML tag for empty values, or a full tag with escaped content otherwise.
     * e.g. xmlTag("read", "") → "<read/>", xmlTag("read", "a") → "<read>a</read>"
     */
    fun xmlTag(tag: String, value: String): String =
        if (value.isEmpty()) "<$tag/>" else "<$tag>${escapeXml(value)}</$tag>"

    /**
     * Formats a float value with 2 decimal places for consistent XML output.
     * Uses Locale.US to ensure dot as decimal separator (required for XML/JFF format).
     */
    fun formatFloat(value: Float): String = "%.2f".format(Locale.US, value)
}
