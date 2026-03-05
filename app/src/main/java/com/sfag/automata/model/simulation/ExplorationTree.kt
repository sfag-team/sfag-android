package com.sfag.automata.model.simulation

class TreeNode(
    val id: Int,
    val stateName: String?,
    val depth: Int,
    val children: MutableList<TreeNode> = mutableListOf(),
    var status: SimulationOutcome = SimulationOutcome.ACTIVE,
    val generation: Int = 0
)

data class Branch(val stateName: String?)

class ExplorationTree {
    var root: TreeNode? = null
        private set
    private var nextId = 0
    private val activeNodes = mutableListOf<TreeNode>()
    private var currentGeneration = 0

    fun initialize(stateName: String) {
        nextId = 0
        currentGeneration = 0
        val node = TreeNode(id = nextId++, stateName = stateName, depth = 0, generation = 0)
        root = node
        activeNodes.clear()
        activeNodes.add(node)
    }

    fun expandActive(branches: Map<Int, List<Branch>>) {
        currentGeneration++
        val newActive = mutableListOf<TreeNode>()
        for (node in activeNodes) {
            val children = branches[node.id]
            if (children.isNullOrEmpty()) {
                node.status = SimulationOutcome.REJECTED
                continue
            }
            for (child in children) {
                val childNode = TreeNode(
                    id = nextId++,
                    stateName = child.stateName,
                    depth = node.depth + 1,
                    generation = currentGeneration
                )
                node.children.add(childNode)
                newActive.add(childNode)
            }
        }
        activeNodes.clear()
        activeNodes.addAll(newActive)
    }

    fun markAcceptedPaths(acceptedNodeIds: Set<Int>) {
        val r = root ?: return
        markAcceptedRecursive(r, acceptedNodeIds)
    }

    private fun markAcceptedRecursive(node: TreeNode, acceptedIds: Set<Int>): Boolean {
        if (node.children.isEmpty()) {
            if (node.id in acceptedIds) {
                node.status = SimulationOutcome.ACCEPTED
                return true
            }
            return false
        }
        var anyAccepted = false
        for (child in node.children) {
            if (markAcceptedRecursive(child, acceptedIds)) {
                anyAccepted = true
            }
        }
        if (anyAccepted) {
            node.status = SimulationOutcome.ACCEPTED
        }
        return anyAccepted
    }

    fun markRemainingAsRejected() {
        val r = root ?: return
        markRejectedRecursive(r)
    }

    private fun markRejectedRecursive(node: TreeNode) {
        if (node.status == SimulationOutcome.ACTIVE) {
            node.status = SimulationOutcome.REJECTED
        }
        for (child in node.children) {
            markRejectedRecursive(child)
        }
    }

    fun getActiveNodes(): List<TreeNode> = activeNodes.toList()

    fun getCurrentGeneration(): Int = currentGeneration

    fun clear() {
        root = null
        nextId = 0
        currentGeneration = 0
        activeNodes.clear()
    }
}
