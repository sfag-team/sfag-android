package com.sfag.grammar.data.local

import android.content.Context
import android.net.Uri
import com.sfag.grammar.core.rule.GrammarRule
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.OutputKeys
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.TransformerFactory
import org.w3c.dom.Document


/**
 * File-based storage for grammar using .jff files (JFLAP format)
 */
internal object GrammarFileStorage {

    /**
     * Save grammar to JFF (JFLAP) format file
     * Format is compatible with JFLAP grammar files
     */
    fun saveToJff(rules: List<GrammarRule>, context: Context, uri: Uri) {
        try {
            val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
            outputStream?.use { stream ->
                val dbFactory = DocumentBuilderFactory.newInstance()
                val docBuilder = dbFactory.newDocumentBuilder()
                val doc: Document = docBuilder.newDocument()

                // Root structure
                val structureElement = doc.createElement("structure")
                doc.appendChild(structureElement)

                // Type - JFLAP grammar type
                val typeElement = doc.createElement("type")
                typeElement.appendChild(doc.createTextNode("grammar"))
                structureElement.appendChild(typeElement)

                // Productions
                rules.forEach { rule ->
                    val productionElement = doc.createElement("production")

                    val leftElement = doc.createElement("left")
                    leftElement.appendChild(doc.createTextNode(rule.left))
                    productionElement.appendChild(leftElement)

                    val rightElement = doc.createElement("right")
                    // Treat epsilon ("ε") as an empty <right/> tag
                    if (rule.right == "ε") {
                        productionElement.appendChild(rightElement) // Empty right
                    } else {
                        rightElement.appendChild(doc.createTextNode(rule.right))
                        productionElement.appendChild(rightElement)
                    }

                    structureElement.appendChild(productionElement)
                }

                // Write the content to output
                val transformerFactory = TransformerFactory.newInstance()
                val transformer = transformerFactory.newTransformer()
                transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
                val source = DOMSource(doc)
                val result = StreamResult(stream)
                transformer.transform(source, result)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
