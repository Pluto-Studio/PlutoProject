package plutoproject.capability.interactive.api.placement.offset

import plutoproject.capability.interactive.api.measuring.Placeable
import plutoproject.capability.interactive.api.util.IntOffset

class OffsetPlaceable(
    val offset: IntOffset,
    val inner: Placeable
) : Placeable by inner {
    override fun placeAt(x: Int, y: Int) {}
}
