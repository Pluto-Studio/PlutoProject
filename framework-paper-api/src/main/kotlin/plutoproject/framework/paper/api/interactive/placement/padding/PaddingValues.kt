package plutoproject.framework.paper.api.interactive.placement.padding

import plutoproject.framework.paper.api.interactive.util.IntOffset

data class PaddingValues(
    val start: Int = 0,
    val end: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
) {
    fun getOffset() = IntOffset(start, top)
}
