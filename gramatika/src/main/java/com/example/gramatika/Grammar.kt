package com.example.gramatika

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.OutputStream
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory



class Grammar : ViewModel() {
    // LiveData to hold the list of grammar rules
    private val _rules = MutableLiveData<List<GrammarRule>>(emptyList())
    val rules: LiveData<List<GrammarRule>> get() = _rules

    private val _terminals = MutableLiveData<Set<Char>>(emptySet())
    val terminals: LiveData<Set<Char>> get() = _terminals

    private val _nonterminals = MutableLiveData<Set<Char>>(emptySet())
    val nonterminals: LiveData<Set<Char>> get() = _nonterminals

    private val _grammarType = MutableLiveData<GrammarType>(GrammarType.UNRESTRICTED)
    val grammarType: LiveData<GrammarType> get() = _grammarType

    private val _isGrammarFinished = MutableLiveData(false)
    val isGrammarFinished: LiveData<Boolean> get() = _isGrammarFinished

    fun toggleGrammarFinished() {
        _isGrammarFinished.value = !(_isGrammarFinished.value ?: false)
    }

    // Method to add a new rule
    fun addRule(left: String, right: String) {
        val newRule = GrammarRule(left, right)
        if (_rules.value?.any { it.left == left && it.right == right } == true) {
            return
        }
        _rules.value = _rules.value?.plus(newRule)
        grammarType(newRule)
        updateSymbols()
    }

    // Method to remove a rule
    fun removeRule(rule: GrammarRule) {
        val currentRules = _rules.value ?: emptyList()
        _rules.value = currentRules.filter { it != rule }
        for(r in currentRules){
            grammarType(r)
        }
        updateSymbols()
    }

    fun updateRule(oldRule: GrammarRule, newLeft: String, newRight: String) {
        val currentRules = _rules.value?.toMutableList() ?: return
        val index = currentRules.indexOf(oldRule)
        if (index != -1) {
            val newRule = GrammarRule(newLeft, newRight)
            currentRules[index] = newRule
            _rules.value = currentRules
            grammarType(newRule)
            updateSymbols()
        }
    }


    fun getIndividualRules(): List<GrammarRule> {
        val individualRules = mutableListOf<GrammarRule>()

        _rules.value?.forEach { rule ->
            rule.right.split('|').forEach { singleRight ->
                individualRules.add(GrammarRule(rule.left, singleRight))
            }
        }

        return individualRules
    }

    private fun updateSymbols() {
        val terminalsSet = mutableSetOf<Char>()
        val nonTerminalsSet = mutableSetOf<Char>()

        // Loop through all rules to classify symbols
        _rules.value?.forEach { rule ->
            for(symbol in rule.left){
                if (!symbol.isDigit() && symbol.isUpperCase()) {
                    nonTerminalsSet.add(symbol)
                } else {
                    terminalsSet.add(symbol)
                }
            }
            for(symbol in rule.right){
                if(symbol == '|' || symbol == 'ε'){
                    continue
                }else if (!symbol.isDigit() && symbol.isUpperCase()) {
                    nonTerminalsSet.add(symbol)
                } else {
                    terminalsSet.add(symbol)
                }
            }
        }

        // Update the LiveData with the new sets
        _nonterminals.value = nonTerminalsSet
        _terminals.value = terminalsSet
    }

    private fun grammarType(rule: GrammarRule) {

        _grammarType.value = when {
            isRegular(rule) -> GrammarType.REGULAR
            isContextFree(rule) -> GrammarType.CONTEXT_FREE
            isContextSensitive(rule) -> GrammarType.CONTEXT_SENSITIVE
            else -> GrammarType.UNRESTRICTED
        }
    }


    // Inside class Grammar : ViewModel()
    fun loadFromXmlUri(context: Context, uri: Uri) {
        _rules.value = emptyList()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use {
                val dbFactory = DocumentBuilderFactory.newInstance()
                val dBuilder = dbFactory.newDocumentBuilder()
                val doc: Document = dBuilder.parse(it)

                doc.documentElement.normalize()
                val nodeList = doc.getElementsByTagName("production")

                if (nodeList.length == 0) {
                    throw IllegalArgumentException("Invalid JFF file: No productions found")
                }

                for (i in 0 until nodeList.length) {
                    val node = nodeList.item(i)
                    if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                        val element = node as Element
                        val leftNode = element.getElementsByTagName("left").item(0)
                        val rightNode = element.getElementsByTagName("right").item(0)

                        if (leftNode != null && rightNode != null) {
                            val left = leftNode.textContent
                            val right = if (rightNode == null || rightNode.textContent.isBlank()) {
                                "ε" // Replace empty productions with epsilon
                            } else {
                                rightNode.textContent
                            }

                            addRule(left, right)
                        } else {
                            throw IllegalArgumentException("Malformed JFF file: Missing 'left' or 'right' elements")
                        }
                    }
                }
            }
        } catch (e: Exception) {

            e.printStackTrace()
        }
    }


    fun saveToJff(outputStream: OutputStream) {
        val rules = _rules.value ?: return

        val xmlContent = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>""")
            append("\n<structure>\n")
            append("\t<type>grammar</type>\n")
            append("\t<!--The list of productions.-->\n")

            for (rule in rules) {
                // If the right side is ε (empty string), write an empty <right/> tag
                append("\t<production>\n")
                append("\t\t<left>${rule.left}</left>\n")
                if (rule.right == "ε") {
                    append("\t\t<right/>\n")
                } else {
                    append("\t\t<right>${rule.right}</right>\n")
                }
                append("\t</production>\n")
            }

            append("</structure>\n")
        }

        outputStream.use { stream ->
            stream.write(xmlContent.toByteArray(Charset.forName("UTF-8")))
        }
    }




}