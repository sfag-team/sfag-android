package com.sfag.automata.domain.model.transition

enum class TapeDirection(val symbol: String) {
    LEFT("L"),
    RIGHT("R"),
    STAY("S");

    override fun toString(): String = symbol

    companion object {
        fun fromSymbol(symbol: String): TapeDirection = when (symbol.uppercase()) {
            "L" -> LEFT
            "R" -> RIGHT
            "S" -> STAY
            else -> RIGHT
        }
    }
}
