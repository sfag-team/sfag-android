package com.sfag.grammar.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.sfag.grammar.data.Jff
import com.sfag.grammar.data.Storage
import com.sfag.grammar.domain.grammar.GrammarRule
import com.sfag.grammar.domain.grammar.GrammarType
import com.sfag.grammar.domain.grammar.classifyGrammar
import com.sfag.main.config.Symbols
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class GrammarViewModel
    @Inject
    internal constructor(
        private val storage: Storage,
    ) : ViewModel() {
        var rules by mutableStateOf<List<GrammarRule>>(emptyList())
            private set

        var terminals by mutableStateOf<Set<Char>>(emptySet())
            private set

        var nonTerminals by mutableStateOf<Set<Char>>(emptySet())
            private set

        var grammarType by mutableStateOf(GrammarType.REGULAR)
            private set

        var isGrammarFinished by mutableStateOf(false)
            private set

        fun toggleGrammarFinished() {
            isGrammarFinished = !isGrammarFinished
        }

        fun addRule(
            left: String,
            right: String,
        ) {
            val newRule = GrammarRule(left, right)
            if (rules.any { it.left == left && it.right == right }) {
                return
            }
            rules = rules + newRule
            reclassifyGrammar()
            updateSymbols()
        }

        fun removeRule(rule: GrammarRule) {
            rules = rules.filter { it != rule }
            reclassifyGrammar()
            updateSymbols()
        }

        fun updateRule(
            rule: GrammarRule,
            newLeft: String,
            newRight: String,
        ) {
            val currentRules = rules.toMutableList()
            val index = currentRules.indexOf(rule)
            if (index != -1) {
                val newRule = GrammarRule(newLeft, newRight)
                currentRules[index] = newRule
                rules = currentRules
                reclassifyGrammar()
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

        /** Persists the current grammar to the fixed auto-save slot. */
        fun autoSave() {
            if (rules.isNotEmpty()) {
                storage.saveGrammar(getIndividualRules())
            }
        }

        /** Loads the auto-saved grammar. Returns true on success. */
        fun loadGrammar(): Boolean {
            val loaded = storage.loadGrammar() ?: return false
            rules = emptyList()
            loaded.forEach { addRule(it.left, it.right) }
            isGrammarFinished = true
            return true
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
                    if (symbol == '|' || "$symbol" == Symbols.EPSILON) {
                        continue
                    } else if (!symbol.isDigit() && symbol.isUpperCase()) {
                        nonTerminalsSet.add(symbol)
                    } else {
                        terminalsSet.add(symbol)
                    }
                }
            }

            nonTerminals = nonTerminalsSet
            terminals = terminalsSet
        }

        private fun reclassifyGrammar() {
            grammarType = classifyGrammar(rules)
        }

        fun loadFromXmlUri(
            context: Context,
            uri: Uri,
        ) {
            context.contentResolver.openInputStream(uri)?.use { loadFromXmlStream(it) }
        }

        fun loadFromXmlStream(inputStream: InputStream) {
            try {
                val parsed = Jff.parse(inputStream)
                rules = emptyList()
                parsed.forEach { addRule(it.left, it.right) }
                toggleGrammarFinished()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
