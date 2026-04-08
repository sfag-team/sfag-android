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
    const val BLANK = "\u2423" // ␣ - blank string
    const val BLANK_CHAR = '\u2423' // ␣ - blank char for tape operations

    // Notations
    const val ARROW = "->" // production rule arrow
}
