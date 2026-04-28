package com.sfag.automata.domain.tree

/** Per-tree-node lifecycle status used by the derivation tree. */
enum class NodeStatus {
    /** Branch is still alive and may take further steps. */
    ACTIVE,

    /** Branch had no transitions available - dead end. */
    DEAD,

    /** Branch ran out of input without reaching an accepting state. */
    REJECTED,

    /** Branch ended in an accepting state. */
    ACCEPTED,
}
