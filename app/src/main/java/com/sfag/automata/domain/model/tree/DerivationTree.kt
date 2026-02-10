package com.sfag.automata.domain.model.tree

data class Branch(val stateName: String?)

class DerivationTree {
    var root: TreeNode? = null
        private set
    private var nextId = 0
    private val activeNodes = mutableListOf<TreeNode>()

    fun initialize(stateName: String) {
        nextId = 0
        val node = TreeNode(id = nextId++, stateName = stateName, depth = 0)
        root = node
        activeNodes.clear()
        activeNodes.add(node)
    }

    fun expandActive(branches: Map<Int, List<Branch>>) {
        val newActive = mutableListOf<TreeNode>()
        for (node in activeNodes) {
            val children = branches[node.id]
            if (children == null || children.isEmpty()) {
                node.status = NodeStatus.REJECTED
                continue
            }
            for (child in children) {
                val childNode = TreeNode(
                    id = nextId++,
                    stateName = child.stateName,
                    depth = node.depth + 1
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
                node.status = NodeStatus.ACCEPTED
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
            node.status = NodeStatus.ACCEPTED
        }
        return anyAccepted
    }

    fun markRemainingAsRejected() {
        val r = root ?: return
        markRejectedRecursive(r)
    }

    private fun markRejectedRecursive(node: TreeNode) {
        if (node.status == NodeStatus.ACTIVE) {
            node.status = NodeStatus.REJECTED
        }
        for (child in node.children) {
            markRejectedRecursive(child)
        }
    }

    fun getActiveNodes(): List<TreeNode> = activeNodes.toList()

    fun clear() {
        root = null
        nextId = 0
        activeNodes.clear()
    }
}
