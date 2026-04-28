package com.sfag.automata.domain.tree

/**
 * Immutable description of a tree-expansion edge. Construct via [Tree.allocateBranch] so the
 * `treeNodeId` matches the id [Tree.expandActive] will assign to the resulting node.
 */
data class Branch(val stateName: String, val treeNodeId: Int)

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

    /**
     * Allocates the next tree-node id and returns a [Branch] tagged with it. The matching node is
     * materialized when the branch is later passed to [expandActive].
     */
    fun allocateBranch(stateName: String): Branch = Branch(stateName, nextId++)

    /**
     * Materializes pre-allocated [Branch]es as children of their parent active nodes. Active nodes
     * whose id is missing from `branches` (or maps to an empty list) are marked DEAD.
     *
     * Branches must come from [allocateBranch] on this tree, or from a prior precompute that was
     * reset and is being replayed on a fresh tree (rebuild path) — both produce stable,
     * non-overlapping ids.
     */
    fun expandActive(branches: Map<Int, List<Branch>>) {
        currentGeneration++
        val newActive = mutableListOf<TreeNode>()
        for (node in activeNodes) {
            val children = branches[node.id]
            if (children.isNullOrEmpty()) {
                node.status = NodeStatus.DEAD
                continue
            }
            for (branch in children) {
                val childNode =
                    TreeNode(
                        id = branch.treeNodeId,
                        stateName = branch.stateName,
                        depth = node.depth + 1,
                    )
                node.children.add(childNode)
                newActive.add(childNode)
                // Keep nextId past every materialized id so subsequent allocateBranch calls can
                // never collide, even on the rebuild path that doesn't go through allocateBranch.
                if (branch.treeNodeId >= nextId) {
                    nextId = branch.treeNodeId + 1
                }
            }
        }
        activeNodes.clear()
        activeNodes.addAll(newActive)
    }

    fun markAcceptedPaths(acceptedNodeIds: Set<Int>) {
        fun walk(node: TreeNode): Boolean {
            if (node.children.isEmpty() && node.id in acceptedNodeIds) {
                node.status = NodeStatus.ACCEPTED
                return true
            }
            // Walk ALL children (no short-circuit) so sibling subtrees that also reach an
            // accepted leaf get their leaves and ancestors marked, instead of being left
            // ACTIVE for markRemainingAsRejected to flip to REJECTED.
            var isAccepting = false
            for (child in node.children) {
                if (walk(child)) {
                    isAccepting = true
                }
            }
            if (isAccepting) {
                node.status = NodeStatus.ACCEPTED
            }
            return isAccepting
        }
        root?.let { walk(it) }
    }

    fun markRemainingAsRejected() {
        fun walk(node: TreeNode): Boolean {
            // Leaf: ACTIVE means alive at sim end but not accepted → REJECTED
            if (node.children.isEmpty()) {
                if (node.status == NodeStatus.ACTIVE) {
                    node.status = NodeStatus.REJECTED
                }
                return node.status == NodeStatus.REJECTED
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
            if (node.status == NodeStatus.ACTIVE) {
                node.status = if (hasRejectedPath) NodeStatus.REJECTED else NodeStatus.DEAD
            }
            return hasRejectedPath || node.status == NodeStatus.REJECTED
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

    fun getCurrentGeneration(): Int = currentGeneration

    fun clear() {
        root = null
        nextId = 0
        currentGeneration = 0
        activeNodes.clear()
    }
}

/**
 * Marks accepting and rejected paths in the tree based on a node-id predicate. Called by the UI
 * layer when simulation ends.
 */
fun Tree.markSimulationEnd(isNodeAccepting: ((Int) -> Boolean)?) {
    if (isNodeAccepting != null) {
        val acceptedIds = getActiveNodes().map { it.id }.filter(isNodeAccepting).toSet()
        markAcceptedPaths(acceptedIds)
    }
    markRemainingAsRejected()
}
