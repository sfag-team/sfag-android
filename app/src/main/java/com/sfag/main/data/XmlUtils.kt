package com.sfag.main.data

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.InputStream
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/** Generic XML utilities used across the app. */
object XmlUtils {
    /** Parses XML from input stream and returns document. */
    fun parseXml(inputStream: InputStream): Document {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return docBuilder.parse(inputStream)
    }

    /** Gets text content of a child element. */
    fun Element.getChildText(tagName: String): String? =
        getElementsByTagName(tagName).item(0)?.textContent

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
}
