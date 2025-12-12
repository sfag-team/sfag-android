package com.sfag.shared.util

/**
 * Standard symbols used in automata theory and formal grammars.
 */
object Symbols {
    // Greek letters for formal definitions
    const val EPSILON_CHAR = '\u03B5' // ε - as Char
    const val EPSILON = "\u03B5"      // ε - empty string
    const val LAMBDA = "\u03BB"       // λ - alternative empty string
    const val DELTA = "\u03B4"        // δ - transition function
    const val SIGMA = "\u03A3"        // Σ - input alphabet
    const val GAMMA = "\u0393"        // Γ - stack/tape alphabet

    // Turing machine
    const val BLANK = 'B'             // B - blank symbol

    // Arrows
    const val ARROW = "->"            // production rule arrow
    const val DERIVES = "=>"          // derivation arrow
}
