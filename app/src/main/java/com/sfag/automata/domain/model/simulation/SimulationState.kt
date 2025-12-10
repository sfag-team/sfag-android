package com.sfag.automata.domain.model

/**
 * Represents the current state of a simulation.
 * This is separate from the Machine definition - Machine is immutable,
 * SimulationState tracks the progress of execution.
 */
sealed class SimulationState {
    abstract val currentStateId: Int?
    abstract val inputPosition: Int
    abstract val isAccepted: Boolean?
    abstract val isFinished: Boolean
    abstract val step: Int

    abstract fun copy(
        currentStateId: Int? = this.currentStateId,
        inputPosition: Int = this.inputPosition,
        isAccepted: Boolean? = this.isAccepted,
        isFinished: Boolean = this.isFinished,
        step: Int = this.step
    ): SimulationState
}

data class FiniteSimulationState(
    override val currentStateId: Int?,
    override val inputPosition: Int,
    override val isAccepted: Boolean?,
    override val isFinished: Boolean,
    override val step: Int = 0,
    val remainingInput: String,
    val originalInput: String
) : SimulationState() {
    override fun copy(
        currentStateId: Int?,
        inputPosition: Int,
        isAccepted: Boolean?,
        isFinished: Boolean,
        step: Int
    ): SimulationState = copy(
        currentStateId = currentStateId,
        inputPosition = inputPosition,
        isAccepted = isAccepted,
        isFinished = isFinished,
        step = step,
        remainingInput = remainingInput,
        originalInput = originalInput
    )
}

data class PushDownSimulationState(
    override val currentStateId: Int?,
    override val inputPosition: Int,
    override val isAccepted: Boolean?,
    override val isFinished: Boolean,
    override val step: Int = 0,
    val remainingInput: String,
    val originalInput: String,
    val stack: List<Char>
) : SimulationState() {
    override fun copy(
        currentStateId: Int?,
        inputPosition: Int,
        isAccepted: Boolean?,
        isFinished: Boolean,
        step: Int
    ): SimulationState = copy(
        currentStateId = currentStateId,
        inputPosition = inputPosition,
        isAccepted = isAccepted,
        isFinished = isFinished,
        step = step,
        remainingInput = remainingInput,
        originalInput = originalInput,
        stack = stack
    )
}

data class TuringSimulationState(
    override val currentStateId: Int?,
    override val inputPosition: Int,
    override val isAccepted: Boolean?,
    override val isFinished: Boolean,
    override val step: Int = 0,
    val tape: MutableList<Char>,
    val headPosition: Int,
    val blankSymbol: Char = '_'
) : SimulationState() {
    override fun copy(
        currentStateId: Int?,
        inputPosition: Int,
        isAccepted: Boolean?,
        isFinished: Boolean,
        step: Int
    ): SimulationState = copy(
        currentStateId = currentStateId,
        inputPosition = inputPosition,
        isAccepted = isAccepted,
        isFinished = isFinished,
        step = step,
        tape = tape,
        headPosition = headPosition,
        blankSymbol = blankSymbol
    )

    fun getTapeString(): String = tape.joinToString("")
}
