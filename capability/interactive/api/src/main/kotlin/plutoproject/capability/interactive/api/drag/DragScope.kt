package plutoproject.capability.interactive.api.drag

import androidx.compose.runtime.Immutable
import org.bukkit.event.inventory.DragType
import org.bukkit.inventory.ItemStack
import plutoproject.capability.interactive.api.util.ItemPositions

@Immutable
data class DragScope(
    val dragType: DragType,
    val updatedItems: ItemPositions,
    var cursor: ItemStack?
)
