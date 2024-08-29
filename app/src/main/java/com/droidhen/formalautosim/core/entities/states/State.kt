package com.droidhen.formalautosim.core.entities.states


class State constructor(
    public val finite: Boolean,
    public val initial: Boolean,
    public val index: Int,
    public var name: String,
    public var isCurrent: Boolean = false,
) {
    var position: Pair<Float, Float> = 0f to 0f

    fun setX(x:Float){
        position = x to position.second
    }

    fun setY(y:Float){
        position = position.first to y
    }
}