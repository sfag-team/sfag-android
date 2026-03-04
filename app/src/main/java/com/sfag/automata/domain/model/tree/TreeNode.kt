package com.sfag.automata.domain.model.tree

class TreeNode(
    val id: Int,
    val stateName: String?,
    val depth: Int,
    val children: MutableList<TreeNode> = mutableListOf(),
    var status: NodeStatus = NodeStatus.ACTIVE,
    val generation: Int = 0
)

enum class NodeStatus { ACTIVE, REJECTED, ACCEPTED }
