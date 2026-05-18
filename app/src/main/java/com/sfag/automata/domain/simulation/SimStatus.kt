package com.sfag.automata.domain.simulation

/** Current status of a running or finished simulation. */
enum class SimStatus {
    /** Simulation is still running - more steps possible. */
    ACTIVE,

    /** Simulation halted without any path reaching an accepting state. */
    REJECTED,

    /** At least one path reached an accepting state. */
    ACCEPTED,
}
