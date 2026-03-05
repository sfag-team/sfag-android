package com.sfag.grammar.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

import com.sfag.grammar.model.GrammarRule
import com.sfag.grammar.model.GrammarType
import com.sfag.grammar.model.isContextFree
import com.sfag.grammar.model.isContextSensitive
import com.sfag.grammar.model.isRegular
import com.sfag.shared.Symbols

class GrammarViewModel : ViewModel() {
    var rules by mutableStateOf<List<GrammarRule>>(emptyList())
        private set

    var terminals by mutableStateOf<Set<Char>>(emptySet())
        private set

    var nonterminals by mutableStateOf<Set<Char>>(emptySet())
        private set

    var grammarType by mutableStateOf(GrammarType.REGULAR)
        private set

    var isGrammarFinished by mutableStateOf(false)
        private set

    fun toggleGrammarFinished() {
        isGrammarFinished = !isGrammarFinished
    }

    fun addRule(left: String, right: String) {
        val newRule = GrammarRule(left, right)
        if (rules.any { it.left == left && it.right == right }) {
            return
        }
        rules = rules + newRule
        for (ruleRight in newRule.right.split('|')) {
            grammarType(GrammarRule(newRule.left, ruleRight))
        }
        updateSymbols()
    }

    fun removeRule(rule: GrammarRule) {
        val newRules = rules.filter { it != rule }
        rules = newRules
        grammarType = GrammarType.REGULAR
        for (r in newRules) {
            grammarType(r)
        }
        updateSymbols()
    }

    fun updateRule(oldRule: GrammarRule, newLeft: String, newRight: String) {
        val currentRules = rules.toMutableList()
        val index = currentRules.indexOf(oldRule)
        if (index != -1) {
            val newRule = GrammarRule(newLeft, newRight)
            currentRules[index] = newRule
            rules = currentRules
            grammarType(newRule)
            updateSymbols()
        }
    }

    fun getIndividualRules(): List<GrammarRule> {
        val individualRules = mutableListOf<GrammarRule>()

        rules.forEach { rule ->
            rule.right.split('|').forEach { singleRight ->
                individualRules.add(GrammarRule(rule.left, singleRight))
            }
        }

        return individualRules
    }

    private fun updateSymbols() {
        val terminalsSet = mutableSetOf<Char>()
        val nonTerminalsSet = mutableSetOf<Char>()

        rules.forEach { rule ->
            for (symbol in rule.left) {
                if (!symbol.isDigit() && symbol.isUpperCase()) {
                    nonTerminalsSet.add(symbol)
                } else {
                    terminalsSet.add(symbol)
                }
            }
            for (symbol in rule.right) {
                if (symbol == '|' || symbol == Symbols.EPSILON) {
                    continue
                } else if (!symbol.isDigit() && symbol.isUpperCase()) {
                    nonTerminalsSet.add(symbol)
                } else {
                    terminalsSet.add(symbol)
                }
            }
        }

        nonterminals = nonTerminalsSet
        terminals = terminalsSet
    }

    private fun grammarType(rule: GrammarRule) {
        val newType = when {
            isRegular(rule) -> GrammarType.REGULAR
            isContextFree(rule) -> GrammarType.CONTEXT_FREE
            isContextSensitive(rule) -> GrammarType.CONTEXT_SENSITIVE
            else -> GrammarType.UNRESTRICTED
        }

        if (newType.priority > grammarType.priority) {
            grammarType = newType
        }
    }

    fun loadFromXmlUri(context: Context, uri: Uri) {
        context.contentResolver.openInputStream(uri)?.use { loadFromXmlStream(it) }
    }

    fun loadFromXmlStream(inputStream: InputStream) {
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(inputStream)

            doc.documentElement.normalize()
            val nodeList = doc.getElementsByTagName("production")
            rules = emptyList()

            for (i in 0 until nodeList.length) {
                val element = nodeList.item(i) as Element
                val left = element.getElementsByTagName("left").item(0).textContent
                val rightNode = element.getElementsByTagName("right").item(0)
                val right = if (rightNode == null || rightNode.textContent.isBlank()) "${Symbols.EPSILON}" else rightNode.textContent

                addRule(left, right)
            }
            toggleGrammarFinished()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
