package com.sfag.automata.domain.tree

import com.sfag.automata.domain.simulation.NodeSnapshot
import com.sfag.automata.domain.simulation.SimulationOutcome

class Branch(val stateName: String) {
    var assignedId: Int = -1
        internal set
}

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
                    TreeNode(id = nextId++, stateName = child.stateName, depth = node.depth + 1)
                child.assignedId = childNode.id
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
            val isAccepting = node.children.any { walk(it) }
            if (isAccepting) {
                node.status = SimulationOutcome.ACCEPTED
            }
            return isAccepting
        }
        root?.let { walk(it) }
    }

    fun markRemainingAsRejected() {
        fun walk(node: TreeNode): Boolean {
            // Leaf: ACTIVE means alive at sim end but not accepted → REJECTED
            if (node.children.isEmpty()) {
                if (node.status == SimulationOutcome.ACTIVE) {
                    node.status = SimulationOutcome.REJECTED
                }
                return node.status == SimulationOutcome.REJECTED
            }

            // Walk ALL children (no short-circuit)
            var hasRejectedPath = false
            for (child in node.children) {
                if (walk(child)) {
                    hasRejectedPath = true
                }
            }

            // ACTIVE interior: REJECTED if any descendant is rejected,
            // otherwise DEAD (all descendants are dead - dead branch)
            if (node.status == SimulationOutcome.ACTIVE) {
                node.status =
                    if (hasRejectedPath) SimulationOutcome.REJECTED else SimulationOutcome.DEAD
            }
            return hasRejectedPath || node.status == SimulationOutcome.REJECTED
        }
        root?.let { walk(it) }
    }

    fun findNode(id: Int): TreeNode? {
        fun walk(node: TreeNode): TreeNode? {
            if (node.id == id) {
                return node
            }
            return node.children.firstNotNullOfOrNull { walk(it) }
        }
        return root?.let { walk(it) }
    }

    fun getActiveNodes(): List<TreeNode> = activeNodes.toList()

    fun attachSnapshots(snapshots: Map<Int, NodeSnapshot>) {
        for (node in activeNodes) {
            snapshots[node.id]?.let { node.snapshot = it }
        }
    }

    fun getCurrentGeneration(): Int = currentGeneration

    fun clear() {
        root = null
        nextId = 0
        currentGeneration = 0
        activeNodes.clear()
    }
}

/**
 * Marks accepting and rejected paths in the tree based on a node predicate. Called by the UI layer
 * when simulation ends.
 */
fun Tree.markSimulationEnd(isNodeAccepting: ((TreeNode) -> Boolean)?) {
    if (isNodeAccepting != null) {
        val acceptedIds = getActiveNodes().filter { isNodeAccepting(it) }.map { it.id }.toSet()
        markAcceptedPaths(acceptedIds)
    }
    markRemainingAsRejected()
}
