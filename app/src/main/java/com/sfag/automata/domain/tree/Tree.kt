package com.sfag.automata.domain.tree

import com.sfag.automata.domain.simulation.SimulationOutcome

data class Branch(
    val stateName: String?,
)

class Tree {
    var root: TreeNode? = null
        private set

    private var nextId = 0
    private val activeNodes = mutableListOf<TreeNode>()
    private var currentGeneration = 0

    fun initialize(stateName: String) {
        nextId = 0
        currentGeneration = 0
        val node = TreeNode(id = nextId++, stateName = stateName, depth = 0)
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
                node.status = SimulationOutcome.DEAD
                continue
            }
            for (child in children) {
                val childNode =
                    TreeNode(
                        id = nextId++,
                        stateName = child.stateName,
                        depth = node.depth + 1,
                    )
                node.children.add(childNode)
                newActive.add(childNode)
            }
        }
        activeNodes.clear()
        activeNodes.addAll(newActive)
    }

    fun markAcceptedPaths(acceptedNodeIds: Set<Int>) {
        val root = root ?: return
        markAcceptedRecursive(root, acceptedNodeIds)
    }

    private fun markAcceptedRecursive(
        node: TreeNode,
        acceptedIds: Set<Int>,
    ): Boolean {
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
        return anyAccepted
    }

    fun markRemainingAsRejected() {
        val root = root ?: return
        markRejectedRecursive(root)
    }

    private fun markRejectedRecursive(node: TreeNode) {
        if (node.status == SimulationOutcome.ACTIVE && node.children.isEmpty()) {
            node.status = SimulationOutcome.REJECTED
        }
        for (child in node.children) {
            markRejectedRecursive(child)
        }
    }

    fun findNode(id: Int): TreeNode? {
        val root = root ?: return null
        return findNodeRecursive(root, id)
    }

    private fun findNodeRecursive(
        node: TreeNode,
        id: Int,
    ): TreeNode? {
        if (node.id == id) return node
        for (child in node.children) {
            findNodeRecursive(child, id)?.let {
                return it
            }
        }
        return null
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
