package com.sfag.automata.core.tree

data class TreeNode(
    val stateName: String?, // null если мёртвый узел
    val weight: Float = 1f,
    val isCurrent: Boolean = false,
    val isAccepted: Boolean = false
)
