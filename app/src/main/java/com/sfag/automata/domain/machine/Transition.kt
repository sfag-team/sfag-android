package com.sfag.automata.domain.machine

import com.sfag.main.config.Symbols

sealed class Transition {
    abstract var name: String
    abstract val fromState: Int
    abstract val toState: Int

    abstract fun displayLabel(): String

    protected fun nameOrEpsilon(): String = name.ifEmpty { Symbols.EPSILON }
}

data class FiniteTransition(
    override var name: String,
    override val fromState: Int,
    override val toState: Int,
) : Transition() {
    override fun displayLabel(): String = nameOrEpsilon()
}

data class PushdownTransition(
    override var name: String,
    override val fromState: Int,
    override val toState: Int,
    var pop: String,
    var push: String,
) : Transition() {
    override fun displayLabel(): String {
        val popStr = pop.ifEmpty { Symbols.EPSILON }
        val pushStr = push.ifEmpty { Symbols.EPSILON }
        return "${nameOrEpsilon()}, $popStr;$pushStr"
    }
}

enum class TapeDirection(
    val symbol: String,
) {
    LEFT("L"),
    RIGHT("R"),
    STAY("S"),
    ;

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
    override var name: String,
    override val fromState: Int,
    override val toState: Int,
    var writeSymbol: Char,
    var direction: TapeDirection,
) : Transition() {
    override fun displayLabel(): String = "${nameOrEpsilon()}/$writeSymbol,${direction.symbol}"
}
