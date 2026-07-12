package plutoproject.capability.interactive.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import plutoproject.capability.interactive.api.click.ClickScope
import plutoproject.capability.interactive.api.drag.DragScope

abstract class GuiInventoryHolder(val scope: GuiInventoryScope) : InventoryHolder {
    var activeInventory: Inventory? by mutableStateOf(null)

    abstract suspend fun processClick(scope: ClickScope, event: Cancellable)

    abstract suspend fun processDrag(scope: DragScope)

    abstract fun onClose(player: Player)

    override fun getInventory(): Inventory =
        activeInventory ?: error("Interactive inventory is used in bukkit but has not been rendered yet.")

    fun close() {
        inventory.viewers.forEach { it.closeInventory(InventoryCloseEvent.Reason.PLUGIN) }
    }
}
