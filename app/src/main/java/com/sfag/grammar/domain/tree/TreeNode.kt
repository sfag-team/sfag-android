package com.sfag.grammar.domain.tree

class TreeNode(
    val step: Int,
    val label: Char,
    val parents: MutableSet<TreeNode> = mutableSetOf(),
    val children: MutableSet<TreeNode> = mutableSetOf()
) {
    fun addChild(child: TreeNode) {
        children.add(child)
        child.parents.add(this)
    }

    override fun toString(): String = label.toString()

    fun getLeaves(): MutableList<TreeNode> {
        val leaves = LinkedHashSet<TreeNode>()

        fun traverse(node: TreeNode) {
            if (node.children.isEmpty()) {
                leaves.add(node)
            } else {
                node.children.forEach(::traverse)
            }
        }
        traverse(this)
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
