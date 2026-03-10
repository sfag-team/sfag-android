package com.sfag.grammar.data

import com.sfag.grammar.domain.grammar.GrammarRule
import com.sfag.main.config.Symbols
import com.sfag.main.data.JffUtils
import com.sfag.main.data.JffUtils.escapeXml
import com.sfag.main.data.JffUtils.xmlTag

fun List<GrammarRule>.exportToJff(): String =
    JffUtils.jffDocument("grammar") {
        this@exportToJff.forEach { rule ->
            appendLine("    <production>")
            appendLine("        <left>${escapeXml(rule.left)}</left>")
            val rightValue = if (rule.right == Symbols.EPSILON) "" else rule.right
            appendLine("        ${xmlTag("right", rightValue)}")
            appendLine("    </production>")
        }
    }
