package com.sfag.automata.domain.model.machine

/**
 * Data class containing machine formal definition for display.
 * Used by UI layer to render the mathematical representation.
 */
data class MachineFormatData(
    val stateNames: List<String>,
    val inputAlphabet: Set<Char>,
    val initialStateName: String,
    val finalStateNames: List<String>,
    val transitionDescriptions: List<String>,
    val machineType: MachineType,
    // PDA-specific fields
    val stackAlphabet: Set<Char>? = null,
    val initialStackSymbol: Char? = null,
    // Turing-specific fields
    val tapeAlphabet: Set<Char>? = null,
    val blankSymbol: Char? = null
)
