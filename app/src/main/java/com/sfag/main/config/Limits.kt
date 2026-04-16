package com.sfag.main.config

const val INITIAL_ZOOM = 0.5f

// Pinch-to-zoom limits
const val MIN_ZOOM = 0.1f
const val MAX_ZOOM = 3.0f

// BFS acceptance checking limits (input editor)
const val MAX_BFS_FA_CONFIGS = 1_000_000
const val MAX_BFS_PDA_CONFIGS = 100_000
const val MAX_BFS_TM_CONFIGS = 100_000
const val MAX_BFS_GRAMMAR_STEPS = 1_000_000

// PDA step-by-step simulation limits
const val MAX_SIM_PDA_CONFIGS = 1_000
const val MAX_SIM_PDA_STALE_STEPS = 100
