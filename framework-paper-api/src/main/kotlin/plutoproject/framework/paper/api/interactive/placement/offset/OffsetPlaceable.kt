package plutoproject.framework.paper.api.interactive.placement.offset

import plutoproject.framework.paper.api.interactive.measuring.Placeable
import plutoproject.framework.paper.api.interactive.util.IntOffset

class OffsetPlaceable(
    val offset: IntOffset,
    val inner: Placeable
) : Placeable by inner {
    override fun placeAt(x: Int, y: Int) {}
}
