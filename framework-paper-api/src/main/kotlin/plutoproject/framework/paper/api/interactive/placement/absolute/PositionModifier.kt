package plutoproject.framework.paper.api.interactive.placement.absolute

import plutoproject.framework.paper.api.interactive.modifiers.LayoutChangingModifier
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.util.IntOffset

class PositionModifier(
    val x: Int = 0,
    val y: Int = 0,
) : Modifier.Element<PositionModifier>, LayoutChangingModifier {
    override fun mergeWith(other: PositionModifier) = other

    override fun modifyPosition(offset: IntOffset) = IntOffset(this.x, this.y)
}

/** Places an element at an absolute offset in the inventory. */
fun Modifier.at(x: Int = 0, y: Int = 0) = then(PositionModifier(x, y))
