package plutoproject.capability.interactive.api.placement.padding

import plutoproject.capability.interactive.api.util.IntOffset

data class PaddingValues(
    val start: Int = 0,
    val end: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
) {
    fun getOffset() = IntOffset(start, top)
}
