package ink.pmc.interactive.api.gui.inventory.modifiers.placement.padding

import ink.pmc.interactive.api.gui.inventory.components.state.IntOffset

data class PaddingValues(
    val start: Int = 0,
    val end: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
) {
    fun getOffset() = IntOffset(start, top)
}
