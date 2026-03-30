package com.sfag.automata.domain.tree

import com.sfag.automata.domain.machine.State
import com.sfag.automata.domain.simulation.SimulationOutcome
import com.sfag.automata.domain.simulation.TransitionRef

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
            val isAccepting = node.children.any { walk(it) }
            if (isAccepting) node.status = SimulationOutcome.ACCEPTED
            return isAccepting
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
 * Expands the derivation tree with exactly the transitions being processed in this step.
 * Called by the UI layer after receiving a simulation step result.
 */
fun Tree.expandFromStep(
    transitionRefs: List<TransitionRef>,
    states: List<State>,
    keepActive: Boolean = false,
) {
    val active = getActiveNodes()
    if (active.isEmpty()) return

    val branches = mutableMapOf<Int, List<Branch>>()
    for (node in active) {
        val state = states.firstOrNull { it.name == node.stateName }
        if (state == null) {
            branches[node.id] = emptyList()
            continue
        }
        val toStateIndices =
            transitionRefs
                .filter { it.fromStateIndex == state.index }
                .map { it.toStateIndex }
                .toMutableList()
        if (keepActive && state.index !in toStateIndices) toStateIndices.add(state.index)
        if (toStateIndices.isEmpty()) {
            branches[node.id] = emptyList()
            continue
        }
        branches[node.id] =
            toStateIndices
                .mapNotNull { index ->
                    states.firstOrNull { it.index == index }?.let { Branch(it.name) }
                }
                .sortedBy { it.stateName }
    }
    expandActive(branches)
}

/**
 * Marks accepting and rejected paths in the tree based on a node predicate.
 * Called by the UI layer when simulation ends.
 */
fun Tree.markSimulationEnd(isNodeAccepting: ((TreeNode) -> Boolean)?) {
    if (isNodeAccepting != null) {
        val acceptedIds = getActiveNodes()
            .filter { isNodeAccepting(it) }
            .map { it.id }
            .toSet()
        markAcceptedPaths(acceptedIds)
    }
    markRemainingAsRejected()
}
