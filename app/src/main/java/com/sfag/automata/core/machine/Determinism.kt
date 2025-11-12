package com.sfag.automata.core.machine

/**
 * Skontroluje, či je daný konečný automat deterministický (DFA).
 *
 * Podmienky:
 *  - stroj je typu Finite
 *  - presne jeden počiatočný stav
 *  - žiadne epsilon prechody
 *  - pre každú dvojicu (startState, symbol) existuje nanajvýš jeden prechod
 */
fun Machine.isDeterministicFinite(): Boolean {
    if (machineType != MachineType.Finite) return false

    // 1) presne jeden initial state
    if (states.count { it.initial } != 1) return false

    // 2+3) kontrola epsilonov a duplicít na (startState, symbol)
    val seen = mutableSetOf<Pair<Int, Char>>()
    val epsilonChars = setOf('ε', 'λ') // uprav podľa toho, čo používaš

    for (t in transitions) {
        val label = t.name

        // prázdny label = epsilon
        if (label.isEmpty()) return false

        // Každý znak v name berieme ako samostatný symbol
        for (ch in label.toSet()) {
            // epsilon symbol v labeli -> nedeterministický
            if (ch in epsilonChars) return false

            val key = t.startState to ch
            if (!seen.add(key)) {
                // už existuje prechod z tohto startState s rovnakým symbolom
                return false
            }
        }
    }

    return true
}
