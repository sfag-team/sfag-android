package com.sfag.automata

import androidx.compose.ui.unit.dp

// Base geometric size of automata nodes
const val NODE_RADIUS = 48f

// Layout heights
val AUTOMATA_CANVAS_HEIGHT = 384.dp
val DERIVATION_TREE_HEIGHT = 256.dp
val TAPE_BAR_HEIGHT = 56.dp

// Tape cell dimensions
val TAPE_CELL_SIZE = 48.dp
val TAPE_CELL_PADDING = (TAPE_BAR_HEIGHT - TAPE_CELL_SIZE) / 2

// Shared node rendering constants - used by both automata diagram and derivation tree.
// Visual size differences come from zoom/scale, not duplicate constants.
const val NODE_OUTLINE_WIDTH = 10f
const val NODE_TEXT_SIZE = 16f
const val NODE_INNER_RADIUS = NODE_RADIUS * 0.8f
const val NODE_INNER_OUTLINE_WIDTH = 5f
const val EDGE_LINE_WIDTH = 3f
const val EDGE_TEXT_SIZE = 16f

// Derivation tree layout constants (dp) - spacing between node centers
const val TREE_PADDING = 16f
const val TREE_NODE_VERTICAL_SPACING = 48f
const val TREE_NODE_HORIZONTAL_SPACING = 48f
