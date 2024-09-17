package com.droidhen.formalautosim.core.entities.states

import androidx.compose.ui.geometry.Offset


class State constructor(
    public val finite: Boolean,
    public val initial: Boolean,
    public val index: Int,
    public var name: String,
    public var isCurrent: Boolean = false,
) {
    var position: Offset= Offset(0f,0f)
    var radius:Float = 40f

    fun setX(x:Float){
        position = Offset(x, position.y)
    }

    fun setY(y:Float){
        position = Offset(position.x, y)
    }
}