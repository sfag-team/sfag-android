package com.sfag.automata.domain.model.machine

sealed class MachineType(val tag: String) {
    data object Finite : MachineType("fa")
    data object Pushdown : MachineType("pda")
    data object Turing : MachineType("turing")

    override fun toString(): String = tag

    companion object {
        fun fromTag(tag: String): MachineType = when (tag) {
            "fa" -> Finite
            "pda" -> Pushdown
            "turing" -> Turing
            else -> Finite
        }
    }
}
