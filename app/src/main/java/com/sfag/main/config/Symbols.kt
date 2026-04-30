package com.sfag.main.config

/** Standard symbols used in automata theory and formal grammars. */
object Symbols {
    // Greek letters
    const val EPSILON = "\u03B5" // ε - empty string
    const val LAMBDA = "\u03BB" // λ - alternative empty string
    const val SIGMA = "\u03A3" // Σ - input alphabet
    const val GAMMA = "\u0393" // Γ - stack/tape alphabet
    const val DELTA = "\u03B4" // δ - transition function

    // Special symbols
    const val BLANK = "\u25A1" // □ - blank string
    const val BLANK_CHAR = '\u25A1' // □ - blank char for tape operations
    const val INITIAL_STACK_SYMBOL = 'Z' // PDA initial stack symbol

    // Notations
    const val PRODUCTION = "\u2192" // → - grammar production rule arrow
}
