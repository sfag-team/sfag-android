package com.sfag.grammar.data

import android.content.Context
import android.net.Uri
import com.sfag.grammar.domain.model.rule.GrammarRule
import com.sfag.shared.util.JffFileUtils
import com.sfag.shared.util.Symbols


/**
 * Grammar-specific JFF file operations.
 * Uses shared JffFileUtils for common operations.
 */
internal object GrammarFileStorage {

    /**
     * Save grammar to JFF (JFLAP) format file.
     */
    fun saveToJff(rules: List<GrammarRule>, context: Context, uri: Uri) {
        try {
            val doc = JffFileUtils.createDocument()

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
                if (rule.right == Symbols.EPSILON) {
                    productionElement.appendChild(rightElement)
                } else {
                    rightElement.appendChild(doc.createTextNode(rule.right))
                    productionElement.appendChild(rightElement)
                }

                structureElement.appendChild(productionElement)
            }

            JffFileUtils.writeXmlToUri(context, doc, uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
