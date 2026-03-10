package com.sfag.grammar.ui.tree

import com.sfag.grammar.domain.grammar.DerivationStep
import com.sfag.grammar.domain.grammar.findReplacementIndex
import com.sfag.grammar.domain.tree.TreeNode
import com.sfag.main.config.Symbols
import java.util.IdentityHashMap

private const val UNRESTRICTED_NODE_SPACING = 120f
private const val UNRESTRICTED_LAYER_HEIGHT = 150f
private const val RESTRICTED_NODE_SPACING = 100f
private const val RESTRICTED_LAYER_HEIGHT = 150f

class NodeLayout(
    var x: Float = Float.NaN,
    var y: Float = 0f,
    val depth: MutableSet<Float> = LinkedHashSet()
)

internal fun buildTree(steps: List<DerivationStep>): TreeNode {
    val root = TreeNode(0, 'S', LinkedHashSet(), LinkedHashSet())
    var currentLeaves = mutableListOf(root)

    for ((stepIndex, step) in steps.withIndex()) {
        val replaceIndex = findReplacementIndex(step.appliedRule, step.previous, step.derived)
        if (replaceIndex < 0 || replaceIndex + step.appliedRule.left.length > currentLeaves.size) continue
        val newChildren =
            step.appliedRule.right.mapTo(LinkedHashSet()) { symbol ->
                TreeNode(stepIndex + 1, symbol, LinkedHashSet(), LinkedHashSet())
            }

        for (i in 0..<step.appliedRule.left.length) {
            val targetNode = currentLeaves[replaceIndex + i]
            for (child in newChildren) {
                targetNode.addChild(child)
            }
        }
        currentLeaves =
            root.getLeaves().filterNot { it.label.toString() == Symbols.EPSILON }.toMutableList()
    }
    return root
}

internal fun layoutPrettyTreeUnrestricted(
    root: TreeNode,
    nodeSpacing: Float = UNRESTRICTED_NODE_SPACING,
    layerHeight: Float = UNRESTRICTED_LAYER_HEIGHT
): Map<TreeNode, NodeLayout> {
    val positions = IdentityHashMap<TreeNode, NodeLayout>()

    fun pos(node: TreeNode) = positions.getOrPut(node) { NodeLayout() }
    var nextX = 0f

    fun firstWalk(
        node: TreeNode,
        depth: Int = 0,
        visited: MutableSet<TreeNode> = mutableSetOf()
    ) {
        if (!visited.add(node)) {
            val nodePos = pos(node)
            val maxParentDepth = node.parents.flatMap { pos(it).depth }.maxOrNull()
            if (maxParentDepth == null) {
                return
            } else {
                nodePos.depth.clear()
                nodePos.depth.add(maxParentDepth + layerHeight)
            }
            return
        }
        pos(node).depth.add(depth * layerHeight)

        val children = node.children
        if (children.isEmpty()) {
            val nodePos = pos(node)
            if (nodePos.x.isNaN()) {
                nodePos.x = nextX
                nextX += nodeSpacing
            }
        } else {
            children.forEach { firstWalk(it, depth + 1, visited) }

            val firstChild = children.first()
            val lastChild = children.last()
            val parentCount = firstChild.parents.size
            if (children.size < parentCount && firstChild.parents.first() == node) {
                val space = (parentCount - 1) * nodeSpacing
                val childrenSpace = (children.size - 1) * nodeSpacing
                var start = (space - childrenSpace) / 2f
                for (it in children) {
                    pos(it).x += start
                    start += nodeSpacing
                }
            }
            if (firstChild.parents.size == 1) {
                pos(node).x = (pos(firstChild).x + pos(lastChild).x) / 2
            } else {
                @Suppress("NAME_SHADOWING")
                val parentCount = lastChild.parents.size
                val nodeIndex = lastChild.parents.indexOf(node)
                val centerOffset = (parentCount - 1) / 2f
                val offset = (nodeIndex - centerOffset) * nodeSpacing
                pos(node).x = (pos(firstChild).x + pos(lastChild).x) / 2f + offset
            }
        }
    }
    firstWalk(root)

    fun secondWalk(
        node: TreeNode,
        layerHeight: Float,
        visited: MutableSet<TreeNode> = mutableSetOf()
    ) {
        if (!visited.add(node)) return
        node.children.forEach { secondWalk(it, layerHeight, visited) }
        if (node.children.isNotEmpty()) {
            val commonDepth =
                node.children
                    .map { pos(it).depth }
                    .reduceOrNull { acc, set -> acc.intersect(set).toMutableSet() }
                    ?.maxOrNull()
            if (commonDepth != null) {
                pos(node).depth.add(commonDepth - layerHeight)
            }
        }
    }
    secondWalk(root, layerHeight)

    return positions
}

internal fun layoutPrettyTreeRestricted(
    root: TreeNode,
    nodeSpacing: Float = RESTRICTED_NODE_SPACING,
    layerHeight: Float = RESTRICTED_LAYER_HEIGHT
): Map<TreeNode, NodeLayout> {
    val positions = IdentityHashMap<TreeNode, NodeLayout>()

    fun pos(node: TreeNode) = positions.getOrPut(node) { NodeLayout() }
    var nextX = 0f

    fun firstWalk(
        node: TreeNode,
        depth: Int = 0
    ) {
        pos(node).depth.add(depth * layerHeight)

        val children = node.children
        if (children.isEmpty()) {
            pos(node).x = nextX
            nextX += nodeSpacing
        } else {
            children.forEach { firstWalk(it, depth + 1) }
            val firstChild = children.first()
            val lastChild = children.last()
            pos(node).x = (pos(firstChild).x + pos(lastChild).x) / 2f
        }
    }
    firstWalk(root)

    return positions
}
