package com.droidhen.formalautosim.utils.extensions

import androidx.compose.ui.geometry.Rect

fun Rect.expandBy(padding: Float): Rect {
    return Rect(
        left = this.left - padding,
        top = this.top - padding,
        right = this.right + padding,
        bottom = this.bottom + padding
    )
}