package com.sfag.automata.core.state

import androidx.compose.ui.geometry.Offset


class State(
    var finite: Boolean,
    var initial: Boolean,
    var index: Int,
    var name: String,
    var isCurrent: Boolean = false,
    var position: Offset= Offset(0f, 0f)
) {
    var radius:Float = 40f

    fun setX(x: Float){
        position = Offset(x, position.y)
    }

    fun setY(y:Float){
        position = Offset(position.x, y)
    }

    override fun toString(): String {
        return name
    }
}
