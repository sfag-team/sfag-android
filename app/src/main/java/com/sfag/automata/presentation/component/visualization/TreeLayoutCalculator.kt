package com.sfag.automata.presentation.component.visualization

import androidx.compose.ui.geometry.Offset
import com.sfag.automata.domain.model.tree.DerivationTree
import com.sfag.automata.domain.model.tree.TreeNode
import com.sfag.automata.presentation.component.TREE_NODE_HORIZONTAL_SPACING
import com.sfag.automata.presentation.component.TREE_NODE_VERTICAL_SPACING
import com.sfag.automata.presentation.component.TREE_PADDING

fun calculateLayout(tree: DerivationTree): Map<Int, Offset> {
    val root = tree.root ?: return emptyMap()
    val positions = mutableMapOf<Int, Offset>()
    var leafIndex = 0

    fun layoutNode(node: TreeNode): Pair<Float, Float> {
        val x = node.depth * TREE_NODE_HORIZONTAL_SPACING + TREE_PADDING

        if (node.children.isEmpty()) {
            val y = leafIndex * TREE_NODE_VERTICAL_SPACING + TREE_PADDING
            leafIndex++
            positions[node.id] = Offset(x, y)
            return y to y
        }

        var minChildY = Float.MAX_VALUE
        var maxChildY = Float.MIN_VALUE
        for (child in node.children) {
            val (childMin, childMax) = layoutNode(child)
            if (childMin < minChildY) minChildY = childMin
            if (childMax > maxChildY) maxChildY = childMax
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
        for ((id, pos) in positions.toMap()) {
            positions[id] = Offset(pos.x, pos.y - midY)
        }
    }

    return positions
}
