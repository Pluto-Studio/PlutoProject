package plutoproject.framework.paper.api.interactive.drag

import androidx.compose.runtime.Immutable
import org.bukkit.event.inventory.DragType
import org.bukkit.inventory.ItemStack
import plutoproject.framework.paper.api.interactive.util.ItemPositions

@Immutable
data class DragScope(
    val dragType: DragType,
    val updatedItems: ItemPositions,
    var cursor: ItemStack?
)
