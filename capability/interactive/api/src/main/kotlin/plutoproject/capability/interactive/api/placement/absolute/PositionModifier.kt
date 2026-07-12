package plutoproject.capability.interactive.api.placement.absolute

import plutoproject.capability.interactive.api.modifiers.LayoutChangingModifier
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.util.IntOffset

class PositionModifier(
    val x: Int = 0,
    val y: Int = 0,
) : Modifier.Element<PositionModifier>, LayoutChangingModifier {
    override fun mergeWith(other: PositionModifier) = other

    override fun modifyPosition(offset: IntOffset) = IntOffset(this.x, this.y)
}

/** Places an element at an absolute offset in the inventory. */
fun Modifier.at(x: Int = 0, y: Int = 0) = then(PositionModifier(x, y))
