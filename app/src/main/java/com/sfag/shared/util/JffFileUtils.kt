package com.sfag.shared.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Shared utilities for JFF (JFLAP) file operations.
 * Used by both automata and grammar modules.
 */
object JffFileUtils {

    /**
     * Shares JFF file via Android share sheet.
     */
    fun shareFile(context: Context, jffContent: String, filename: String, shareMessage: String = "Share file") {
        val file = File(context.cacheDir, "$filename.jff")
        file.writeText(jffContent)

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, shareMessage))
    }

    /**
     * Writes XML document to an output stream with formatting.
     */
    fun writeXmlToStream(doc: Document, outputStream: OutputStream) {
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        val source = DOMSource(doc)
        val result = StreamResult(outputStream)
        transformer.transform(source, result)
    }

    /**
     * Writes XML document to a URI.
     */
    fun writeXmlToUri(context: Context, doc: Document, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            writeXmlToStream(doc, stream)
        }
    }

    /**
     * Parses XML from string and returns document.
     */
    fun parseXml(xmlContent: String): Document {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return docBuilder.parse(xmlContent.byteInputStream())
    }

    /**
     * Creates a new XML document.
     */
    fun createDocument(): Document {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = dbFactory.newDocumentBuilder()
        return docBuilder.newDocument()
    }

    /**
     * Gets the JFF type from document (fa, pda, turing, grammar).
     */
    fun getJffType(doc: Document): String {
        val typeElement = doc.documentElement.getElementsByTagName("type").item(0)
        return typeElement?.textContent?.trim()?.lowercase() ?: "fa"
    }

    /**
     * Normalizes epsilon (ε) symbols to empty string.
     * JFLAP uses various representations: empty string, ε, λ, "eps", "epsilon"
     */
    fun normalizeEpsilon(value: String): String {
        val trimmed = value.trim()
        return when (trimmed.lowercase()) {
            "", Symbols.EPSILON, Symbols.LAMBDA, "eps", "epsilon" -> ""
            else -> trimmed
        }
    }

    /**
     * Helper to get text content from child element.
     */
    fun Element.getChildText(tagName: String): String? {
        return getElementsByTagName(tagName).item(0)?.textContent
    }

    /**
     * Helper to check if child element exists.
     */
    fun Element.hasChild(tagName: String): Boolean {
        return getElementsByTagName(tagName).length > 0
    }
}
