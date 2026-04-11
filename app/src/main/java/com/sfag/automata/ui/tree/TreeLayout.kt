package com.sfag.automata.ui.tree

import androidx.compose.ui.geometry.Offset
import com.sfag.automata.domain.tree.Tree
import com.sfag.automata.domain.tree.TreeNode
import com.sfag.automata.ui.common.NODE_RADIUS

private const val DEPTH_SPACING = NODE_RADIUS * 1.5f
private const val LEAF_SPACING = NODE_RADIUS * 1f

internal fun calculateLayout(tree: Tree): Map<Int, Offset> {
    val root = tree.root ?: return emptyMap()
    val positions = mutableMapOf<Int, Offset>()
    var leafPosition = 0

    fun layoutNode(node: TreeNode): Pair<Float, Float> {
        val x = node.depth * DEPTH_SPACING

        if (node.children.isEmpty()) {
            val y = leafPosition * LEAF_SPACING
            leafPosition++
            positions[node.id] = Offset(x, y)
            return y to y
        }

        var minChildY = Float.MAX_VALUE
        var maxChildY = Float.MIN_VALUE
        for (child in node.children) {
            val (childMin, childMax) = layoutNode(child)
            if (childMin < minChildY) {
                minChildY = childMin
            }
            if (childMax > maxChildY) {
                maxChildY = childMax
            }
        }

        val y = (minChildY + maxChildY) / 2f
        positions[node.id] = Offset(x, y)
        return minChildY to maxChildY
    }

    layoutNode(root)

    // Center tree vertically (shift so midpoint of all Y positions is at 0)
    if (positions.isNotEmpty()) {
        val minY = positions.values.minOf { it.y }
        val maxY = positions.values.maxOf { it.y }
        val midY = (minY + maxY) / 2f
        for ((id, offset) in positions) {
            positions[id] = Offset(offset.x, offset.y - midY)
        }
    }

    return positions
}
