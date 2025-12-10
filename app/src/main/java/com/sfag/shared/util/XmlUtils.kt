package com.sfag.shared.util

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
     * Formats a float value with 2 decimal places for consistent XML output.
     */
    fun formatFloat(value: Float): String = "%.2f".format(value)
}
