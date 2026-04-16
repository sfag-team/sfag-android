package com.sfag.automata.domain.tree

import com.sfag.automata.domain.simulation.NodeSnapshot
import com.sfag.automata.domain.simulation.SimulationOutcome

class TreeNode(
    val id: Int,
    val stateName: String,
    val depth: Int,
    val children: MutableList<TreeNode> = mutableListOf(),
    var status: SimulationOutcome = SimulationOutcome.ACTIVE,
    var snapshot: NodeSnapshot? = null,
)
