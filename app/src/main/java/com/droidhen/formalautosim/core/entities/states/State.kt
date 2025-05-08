package com.droidhen.formalautosim.core.entities.states

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp


class State constructor(
    public var finite: Boolean,
    public var initial: Boolean,
    public var index: Int,
    public var name: String,
    public var isCurrent: Boolean = false,
    public var position: Offset= Offset(0f,0f)
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