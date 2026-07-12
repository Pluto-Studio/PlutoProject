package plutoproject.capability.interactive.paper.inventory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import plutoproject.framework.common.util.chat.catchInteractiveException
import plutoproject.capability.interactive.api.GuiInventoryHolder
import plutoproject.capability.interactive.api.click.ClickScope

class InventoryListener(private val moduleScope: CoroutineScope) : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val invHolder = event.inventory.holder as? GuiInventoryHolder ?: return

        if (event.click !in setOf(LEFT, RIGHT, MIDDLE)) event.isCancelled = true

        val clickedInventory = event.clickedInventory ?: return
        if (clickedInventory.holder !== invHolder) return
        event.isCancelled = true

        val scope = ClickScope(
            event.view,
            event.click,
            event.slot,
            event.cursor.takeIf { it.type != Material.AIR },
            event.whoClicked,
        )
        moduleScope.launch {
            catchInteractiveException(event.whoClicked) {
                invHolder.processClick(scope, event)
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder as? GuiInventoryHolder ?: return
        val scope = holder.scope

        if (scope.isPendingRefresh.value) {
            scope.setPendingRefreshIfNeeded(false)
            return
        }

        holder.onClose(event.player as Player)
        scope.dispose()
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val invHolder = event.inventory.holder as? GuiInventoryHolder ?: return
        val inInv = event.newItems.filter { it.key < event.view.topInventory.size }
        if (event.newItems.size == 1 && inInv.size == 1) {
            event.isCancelled = true
            val clicked = inInv.entries.first()
            val scope = ClickScope(
                event.view,
                LEFT,
                clicked.key,
                event.cursor?.takeIf { it.type != Material.AIR },
                event.whoClicked,
            )
            moduleScope.launch {
                catchInteractiveException(event.whoClicked) {
                    invHolder.processClick(scope, event)
                }
            }
        } else if (inInv.isNotEmpty()) {
            event.isCancelled = true
        }
    }
}
