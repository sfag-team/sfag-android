package com.sfag.automata.domain.simulation

/** Outcome of a simulation path or the overall simulation. */
enum class SimulationOutcome {
    /** Simulation is still running - more steps possible. */
    ACTIVE,

    /** No transitions available from this branch - dead-end in derivation tree. */
    DEAD,

    /** All input consumed but not in an accepting state. */
    REJECTED,

    /** All input consumed and in an accepting state. */
    ACCEPTED,
}
