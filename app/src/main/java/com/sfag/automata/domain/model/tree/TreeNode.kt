package com.sfag.automata.domain.model.tree

data class TreeNode(
    val stateName: String?, // null if dead node
    val weight: Float = 1f,
    val isCurrent: Boolean = false,
    val isAccepted: Boolean = false
)
