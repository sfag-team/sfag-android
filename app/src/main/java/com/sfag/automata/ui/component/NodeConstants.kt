package com.sfag.automata.ui.component

import com.sfag.automata.domain.model.NODE_OUTLINE_WIDTH
import com.sfag.automata.domain.model.NODE_RADIUS

// Node rendering constants (base sizes, used in sim diagram DrawScope)
const val NODE_TEXT_SIZE = 16
const val NODE_INNER_RADIUS = NODE_RADIUS * 0.8f
const val NODE_INNER_OUTLINE_WIDTH = 5f
const val EDGE_LINE_WIDTH = 3f
const val EDGE_TEXT_SIZE = 20f

// Derivation tree scale factor (relative to base node size)
const val TREE_SCALE = 0.75f

// Derivation tree node rendering (derived from base × TREE_SCALE)
const val TREE_NODE_RADIUS = NODE_RADIUS * TREE_SCALE
const val TREE_NODE_OUTLINE_WIDTH = NODE_OUTLINE_WIDTH * TREE_SCALE
const val TREE_NODE_TEXT_SIZE = 12f
const val TREE_EDGE_LINE_WIDTH = 2f

// Derivation tree layout constants (dp)
const val TREE_PADDING = 16f
const val TREE_NODE_VERTICAL_SPACING = 48f
const val TREE_NODE_HORIZONTAL_SPACING = 48f
