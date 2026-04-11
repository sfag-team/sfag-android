package com.sfag.automata.domain.machine

import com.sfag.main.config.Symbols

sealed class Transition {
    abstract val fromState: Int
    abstract val toState: Int
    abstract var read: String

    abstract fun displayLabel(): String

    protected fun displayEpsilon(value: String): String = value.ifEmpty { Symbols.EPSILON }
}

data class FiniteTransition(
    override val fromState: Int,
    override val toState: Int,
    override var read: String,
) : Transition() {
    override fun displayLabel(): String = displayEpsilon(read)
}

data class PushdownTransition(
    override val fromState: Int,
    override val toState: Int,
    override var read: String,
    var pop: String,
    var push: String,
) : Transition() {
    override fun displayLabel(): String {
        val readLabel = displayEpsilon(read)
        val popLabel = displayEpsilon(pop)
        val pushLabel = displayEpsilon(push)
        return "$readLabel, $popLabel ${Symbols.ARROW} $pushLabel"
    }
}

enum class TapeDirection(val symbol: String) {
    LEFT("L"),
    RIGHT("R"),
    STAY("S");

    override fun toString(): String = symbol

    companion object {
        fun fromSymbol(symbol: String): TapeDirection =
            when (symbol.uppercase()) {
                "L" -> LEFT
                "R" -> RIGHT
                "S" -> STAY
                else -> RIGHT
            }
    }
}

data class TuringTransition(
    override val fromState: Int,
    override val toState: Int,
    override var read: String,
    var write: Char,
    var direction: TapeDirection,
) : Transition() {
    override fun displayLabel(): String {
        val readLabel = displayEpsilon(read)
        return "$readLabel/$write,${direction.symbol}"
    }
}
