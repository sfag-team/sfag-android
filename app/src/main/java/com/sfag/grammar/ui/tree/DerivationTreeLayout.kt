package com.sfag.grammar.ui.tree

import com.sfag.grammar.model.Step
import com.sfag.grammar.ui.findDifferenceIndex

class DerivationNode(
    val step: Int,
    val label: Char,
    val parents: MutableSet<DerivationNode> = mutableSetOf(),
    val children: MutableSet<DerivationNode> = mutableSetOf(),
) {
    fun addChild(child: DerivationNode) {
        children.add(child)
        child.parents.add(this)
    }
    override fun toString(): String {
        return label.toString()
    }
    // Layout
    var x: Float = 1f
    var y: Float = 0f
    val depth: MutableSet<Float> = LinkedHashSet()
}

fun buildTree(steps: List<Step>): DerivationNode {
    // Init first step
    val root = DerivationNode(0, 'S', LinkedHashSet(), LinkedHashSet())
    var currentLeaves = mutableListOf(root)

    for ((stepIdx, step) in steps.withIndex()) {
        // Find replacement position in previous state
        val replacePos = findDifferenceIndex(step.appliedRule, step.previous, step.stateString)
        // Create new child nodes
        val newChildren = step.appliedRule.right.mapTo(LinkedHashSet()) { symbol ->
            DerivationNode(stepIdx + 1, symbol, LinkedHashSet(), LinkedHashSet())
        }

        for(i in 0..< step.appliedRule.left.length){
            val targetNode = currentLeaves[replacePos+i]
            for (c in newChildren) {
                targetNode.addChild(c)
            }
        }
        currentLeaves = getCurrentLeaves(root)
    }
    return root
}

fun getCurrentLeaves(node: DerivationNode): MutableList<DerivationNode> {
    val leaves = LinkedHashSet<DerivationNode>()
    fun traverse(n: DerivationNode) {
        if (n.children.isEmpty()) {
            leaves.add(n)
        } else {
            n.children.forEach(::traverse)
        }
    }
    traverse(node)
    return leaves.toMutableList()
}

fun layoutPrettyTreeUnrestricted(root: DerivationNode, nodeSpacing: Float = 120f, layerHeight: Float = 150f){
    var nextX = 0f

    // Positioning
    fun firstWalk(node: DerivationNode, depth: Int = 0, visited: MutableSet<DerivationNode> = mutableSetOf()) {
        if (!visited.add(node)){
            val maxParentDepth = node.parents.flatMap { it.depth }.maxOrNull()
            if(maxParentDepth == null){
                return
            }else{
                node.depth.clear()
                node.depth.add(maxParentDepth +layerHeight)
            }
            return
        }
        node.depth.add(depth*layerHeight)

        val children = node.children
        // Set x for leave
        if(children.isEmpty()){
            if(node.x == 1f){
                node.x = nextX
                nextX += nodeSpacing
            }
        }else{
            children.forEach{firstWalk(it, depth+1, visited)}

            val left = children.first()
            val right = children.last()
            val parentsSize = left.parents.size
            // Move children if they have more parents and create space for them
            if(children.size < parentsSize && left.parents.first() == node){
                val space = (parentsSize-1)*nodeSpacing
                val childrenSpace = (children.size-1) * nodeSpacing
                var start =(space-childrenSpace)/2f
                for(it in children){
                    it.x += start
                    start += nodeSpacing
                }
            }
            //Center the parents
            // Single parent
            if(left.parents.size == 1){
                node.x = (left.x + right.x) / 2
            } else{
                // Multiple parent
                val parentCount = right.parents.size
                val nodeIndex = right.parents.indexOf(node)
                val centerOffset = (parentCount - 1) / 2f
                val offset = (nodeIndex - centerOffset) * nodeSpacing
                node.x = (left.x + right.x) / 2f + offset
            }
        }
    }
    firstWalk(root)
    // Set heights
    fun secondWalk(node: DerivationNode, layerHeight: Float, visited: MutableSet<DerivationNode> = mutableSetOf()) {
        if (!visited.add(node)) return  // Skip if already visited

        node.children.forEach { secondWalk(it, layerHeight, visited) }
        if (node.children.isNotEmpty()) {
            val commonDepth = node.children
                .map { it.depth }
                .reduceOrNull { acc, set -> acc.intersect(set).toMutableSet() }
                ?.maxOrNull()

            if (commonDepth != null) {
                node.depth.add(commonDepth - layerHeight)
            }
        }
    }
    secondWalk(root, layerHeight)
}

fun layoutPrettyTreeRestricted(root: DerivationNode, nodeSpacing: Float = 100f, layerHeight: Float = 150f) {
    var nextX = 0f

    fun firstWalk(node: DerivationNode, depth: Int = 0) {
        node.depth.add(depth*layerHeight)

        val children = node.children
        if (children.isEmpty()) {
            node.x = nextX
            nextX += nodeSpacing
        } else {
            children.forEach { firstWalk(it, depth + 1) }
            // Center parent
            val left = children.first()
            val right = children.last()
            node.x = (left.x + right.x) / 2f
        }
    }
    firstWalk(root)
}

fun collect(node: DerivationNode, nodes: MutableSet<DerivationNode>, step: Int) {
    if(node.step > step) return
    if (nodes.add(node)) {
        node.children.forEach { collect(it, nodes, step) }
    }
}
