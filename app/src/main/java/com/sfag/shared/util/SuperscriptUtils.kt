package com.sfag.shared.util

/**
 * Utility for displaying text with superscripts.
 * Converts ASCII caret notation (^n, ^2) to Unicode superscripts for UI display.
 */
object SuperscriptUtils {

    // Common superscripts used in automata theory and formal languages
    private val superscriptMap = mapOf(
        // Numbers - for string powers (a^2, a^3, S^0, S^1, ...)
        '0' to '\u2070',  // ⁰
        '1' to '\u00B9',  // ¹
        '2' to '\u00B2',  // ²
        '3' to '\u00B3',  // ³
        '4' to '\u2074',  // ⁴
        '5' to '\u2075',  // ⁵
        '6' to '\u2076',  // ⁶
        '7' to '\u2077',  // ⁷
        '8' to '\u2078',  // ⁸
        '9' to '\u2079',  // ⁹
        // Operators - for expressions (n+1, n-1)
        '+' to '\u207A',  // ⁺
        '-' to '\u207B',  // ⁻
        // Variables - common in formal language notation
        'n' to '\u207F',  // ⁿ - most common (a^n b^n)
        'i' to '\u2071',  // ⁱ - indices
        'm' to '\u1D50',  // ᵐ - (a^n b^m)
        'k' to '\u1D4F',  // ᵏ - common in proofs
        // Reverse notation
        'R' to '\u1D3F',  // ᴿ - (w^R = reverse of w)
    )

    /**
     * Converts caret notation to Unicode superscripts for display.
     * If a character has no superscript equivalent, keeps the ^x format as fallback.
     *
     * Examples:
     *   "a^n" -> "aⁿ"
     *   "a^n b^n" -> "aⁿ bⁿ"
     *   "a^2" -> "a²"
     *   "ww^R" -> "wwᴿ"
     *   "a^x" -> "a^x" (fallback - no superscript for x)
     */
    fun toDisplayString(text: String): String {
        val result = StringBuilder()
        var i = 0

        while (i < text.length) {
            val c = text[i]

            if (c == '^' && i + 1 < text.length) {
                val superscriptPart = StringBuilder()
                var j = i + 1
                var allConverted = true

                // Collect consecutive convertible characters
                while (j < text.length && text[j] != ' ' && text[j] != '^') {
                    val superChar = superscriptMap[text[j]]
                    if (superChar != null) {
                        superscriptPart.append(superChar)
                        j++
                    } else {
                        allConverted = false
                        break
                    }
                }

                if (allConverted && superscriptPart.isNotEmpty()) {
                    result.append(superscriptPart)
                    i = j
                } else {
                    // Fallback: keep ^x format
                    result.append(c)
                    i++
                }
            } else {
                result.append(c)
                i++
            }
        }

        return result.toString()
    }
}
