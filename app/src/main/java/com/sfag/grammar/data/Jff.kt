package com.sfag.grammar.data

import com.sfag.grammar.domain.grammar.GrammarRule
import com.sfag.main.config.Symbols
import com.sfag.main.data.JffUtils
import com.sfag.main.data.JffUtils.getChildText
import org.w3c.dom.Element
import java.io.InputStream

object Jff {
    fun parse(inputStream: InputStream): List<GrammarRule> {
        val doc = JffUtils.parseXml(inputStream)
        doc.documentElement.normalize()
        val nodeList = doc.getElementsByTagName("production")
        val rules = mutableListOf<GrammarRule>()
        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as? Element ?: continue
            val left = element.getChildText("left") ?: continue
            val rightNode = element.getElementsByTagName("right").item(0)
            val right =
                if (rightNode == null || rightNode.textContent.isBlank()) {
                    Symbols.EPSILON
                } else {
                    rightNode.textContent
                }
            rules.add(GrammarRule(left, right))
        }
        return rules
    }
}
