package com.sfag.automata.domain.tree

class TreeNode(
    val id: Int,
    val stateName: String,
    val depth: Int,
    val children: MutableList<TreeNode> = mutableListOf(),
    var status: NodeStatus = NodeStatus.ACTIVE,
)
