package com.sfag.automata.domain.simulation

/** Overall outcome of a simulation. */
enum class SimulationOutcome {
    /** Simulation is still running - more steps possible. */
    ACTIVE,

    /** Simulation halted without any path reaching an accepting state. */
    REJECTED,

    /** At least one path reached an accepting state. */
    ACCEPTED,
}
