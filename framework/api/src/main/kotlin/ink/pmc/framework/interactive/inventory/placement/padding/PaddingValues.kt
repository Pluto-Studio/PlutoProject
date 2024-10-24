package ink.pmc.framework.interactive.inventory.placement.padding

import ink.pmc.framework.interactive.inventory.state.IntOffset

data class PaddingValues(
    val start: Int = 0,
    val end: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
) {
    fun getOffset() = IntOffset(start, top)
}
