package com.sfag.grammar.data

import android.content.Context
import com.sfag.grammar.domain.grammar.GrammarRule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** File-based storage for grammars using .jff files. */
internal class Storage
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val storageDir: File
            get() {
                val dir = File(context.filesDir, "grammar")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                return dir
            }

        /** Auto-save the current grammar rules to a fixed internal file. */
        fun saveGrammar(rules: List<GrammarRule>) {
            File(storageDir, "__current.jff").writeText(rules.exportToJff())
        }

        /**
         * Load the auto-saved grammar. Returns the rules, or null if no saved grammar exists.
         */
        fun loadGrammar(): List<GrammarRule>? {
            val jffFile = File(storageDir, "__current.jff")
            if (!jffFile.exists()) return null
            return jffFile.inputStream().use { Jff.parse(it) }.ifEmpty { null }
        }
    }
