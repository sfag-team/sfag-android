package com.sfag.automata.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/** Precomputed render data for a single transition. */
data class TransitionRenderData(
    val bodyPath: Path,
    val headPath: Path,
    /** For regular: bezier control point. For self-loop: arc peak (outermost point). */
    val controlPoint: Offset,
    /** Text label anchor: near the curve, offset outward from the chord. */
    val textPosition: Offset,
    val startConnectionOffset: Offset,
    val endConnectionOffset: Offset,
    val isSelfLoop: Boolean = false
)
