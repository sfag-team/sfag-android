package com.sfag.grammar.domain.tree

class TreeNode(
    val step: Int,
    val label: Char,
    val parents: MutableSet<TreeNode> = mutableSetOf(),
    val children: MutableSet<TreeNode> = mutableSetOf()
) {
    override fun toString(): String = label.toString()

    fun addChild(child: TreeNode) {
        children.add(child)
        child.parents.add(this)
    }

    fun getLeaves(): MutableList<TreeNode> {
        val leaves = LinkedHashSet<TreeNode>()
        val visited = mutableSetOf<TreeNode>()
        val stack = ArrayDeque<TreeNode>()
        stack.addLast(this)
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            if (!visited.add(node)) continue
            if (node.children.isEmpty()) {
                leaves.add(node)
            } else {
                node.children.toList().asReversed().forEach { stack.addLast(it) }
            }
        }
        return leaves.toMutableList()
    }

    fun collect(
        nodes: MutableSet<TreeNode>,
        step: Int
    ) {
        if (this.step > step) return
        if (nodes.add(this)) {
            children.forEach { it.collect(nodes, step) }
        }
    }
}
