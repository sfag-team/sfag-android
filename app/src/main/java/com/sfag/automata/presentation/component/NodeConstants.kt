package com.sfag.automata.presentation.component

import com.sfag.automata.domain.model.NODE_RADIUS

// Simulation node constants (px, scaled by graphicsLayer zoom)
const val SIM_NODE_RADIUS = NODE_RADIUS
const val SIM_NODE_OUTLINE_WIDTH = 10f
const val SIM_NODE_TEXT_SIZE = 16
const val SIM_INNER_NODE_RADIUS = SIM_NODE_RADIUS * 0.8f
const val SIM_INNER_NODE_OUTLINE_WIDTH = 5f
const val SIM_EDGE_LINE_WIDTH = 3f

// Derivation tree node constants (px, scaled by DrawScope transforms)
const val TREE_NODE_RADIUS = 48f
const val TREE_NODE_OUTLINE_WIDTH = 10f
const val TREE_NODE_TEXT_SIZE = 16f
const val TREE_EDGE_LINE_WIDTH = 3f

// Derivation tree layout constants (dp)
const val TREE_PADDING = 36f
const val TREE_NODE_VERTICAL_SPACING = TREE_PADDING + 16f
const val TREE_NODE_HORIZONTAL_SPACING = TREE_PADDING + 32f
