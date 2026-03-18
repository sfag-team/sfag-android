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
        fun walk(node: TreeNode): Boolean {
            if (node.children.isEmpty() && node.id in acceptedNodeIds) {
                node.status = SimulationOutcome.ACCEPTED
                return true
            }
            return node.children.any { walk(it) }
        }
        root?.let { walk(it) }
    }

    fun markRemainingAsRejected() {
        fun walk(node: TreeNode) {
            if (node.status == SimulationOutcome.ACTIVE && node.children.isEmpty()) {
                node.status = SimulationOutcome.REJECTED
            }
            node.children.forEach { walk(it) }
        }
        root?.let { walk(it) }
    }

    fun findNode(id: Int): TreeNode? {
        fun walk(node: TreeNode): TreeNode? {
            if (node.id == id) return node
            return node.children.firstNotNullOfOrNull { walk(it) }
        }
        return root?.let { walk(it) }
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
