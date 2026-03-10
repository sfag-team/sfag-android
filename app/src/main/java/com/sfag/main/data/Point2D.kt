package com.sfag.main.data

/** Domain-safe 2D coordinate for JFF import/export. UI layer uses Compose Offset instead. */
data class Point2D(
    val x: Float = 0f,
    val y: Float = 0f,
)
