package com.sfag.automata.domain.machine

/**
 * Immutable construction plan for a [Machine] - everything needed to build a fresh instance,
 * without any of the mutable simulation state (tree, activeConfigs). Safe to pass to background
 * threads.
 */
sealed interface MachineBlueprint {
    val name: String
    val states: List<State>

    fun toMachine(): Machine

    data class Fa(
        override val name: String,
        override val states: List<State>,
        val transitions: List<FaTransition>,
    ) : MachineBlueprint {
        override fun toMachine(): FiniteMachine =
            FiniteMachine(
                name = name,
                states = states.toMutableList(),
                faTransitions = transitions.toMutableList(),
            )
    }

    data class Pda(
        override val name: String,
        override val states: List<State>,
        val transitions: List<PdaTransition>,
        val acceptanceCriteria: AcceptanceCriteria,
    ) : MachineBlueprint {
        override fun toMachine(): PushdownMachine =
            PushdownMachine(
                    name = name,
                    states = states.toMutableList(),
                    pdaTransitions = transitions.toMutableList(),
                )
                .also { it.acceptanceCriteria = acceptanceCriteria }
    }

    data class Tm(
        override val name: String,
        override val states: List<State>,
        val transitions: List<TmTransition>,
        val blankSymbol: Char,
    ) : MachineBlueprint {
        override fun toMachine(): TuringMachine =
            TuringMachine(
                name = name,
                states = states.toMutableList(),
                tmTransitions = transitions.toMutableList(),
                blankSymbol = blankSymbol,
            )
    }
}

/** Captures the machine's static parts as a blueprint - safe to read from background threads. */
fun Machine.toBlueprint(): MachineBlueprint =
    when (this) {
        is FiniteMachine ->
            MachineBlueprint.Fa(
                name = name,
                states = states.toList(),
                transitions = faTransitions.toList(),
            )

        is PushdownMachine ->
            MachineBlueprint.Pda(
                name = name,
                states = states.toList(),
                transitions = pdaTransitions.toList(),
                acceptanceCriteria = acceptanceCriteria,
            )

        is TuringMachine ->
            MachineBlueprint.Tm(
                name = name,
                states = states.toList(),
                transitions = tmTransitions.toList(),
                blankSymbol = blankSymbol,
            )
    }
