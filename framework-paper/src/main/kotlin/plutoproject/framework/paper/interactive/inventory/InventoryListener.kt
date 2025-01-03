package plutoproject.framework.paper.interactive.inventory

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import plutoproject.framework.common.util.catchInteractiveException
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.paper.api.interactive.GuiInventoryHolder
import plutoproject.framework.paper.api.interactive.click.ClickScope

@Suppress("UNUSED", "UnusedReceiverParameter")
object InventoryListener : Listener {
    @EventHandler
    fun InventoryClickEvent.e() {
        val invHolder = inventory.holder as? GuiInventoryHolder ?: return

        // Avoid any exploits shift clicking or double-clicking into/from the GUI
        if (click !in setOf(LEFT, RIGHT, MIDDLE)) isCancelled = true

        val clickedInventory = clickedInventory ?: return
        if (clickedInventory.holder !== invHolder) return
        isCancelled = true

        val scope = ClickScope(view, click, slot, cursor.takeIf { it.type != Material.AIR }, whoClicked)
        runAsync {
            catchInteractiveException(whoClicked) {
                invHolder.processClick(scope, this@e)
            }
        }
    }

    @EventHandler
    fun InventoryCloseEvent.e() {
        val holder = inventory.holder as? GuiInventoryHolder ?: return
        val scope = holder.scope

        if (scope.isPendingRefresh.value) {
            scope.setPendingRefreshIfNeeded(false)
            return
        }

        holder.onClose(player as Player)
        scope.dispose()
    }

    @EventHandler
    fun InventoryDragEvent.e() {
        val invHolder = inventory.holder as? GuiInventoryHolder ?: return
        val inInv = newItems.filter { it.key < view.topInventory.size }
        if (newItems.size == 1 && inInv.size == 1) {
            isCancelled = true
            val clicked = inInv.entries.first()
            val scope = ClickScope(view, LEFT, clicked.key, cursor?.takeIf { it.type != Material.AIR }, whoClicked)
            runAsync {
                catchInteractiveException(whoClicked) {
                    invHolder.processClick(scope, this@e)
                }
            }
        } else if (inInv.isNotEmpty()) {
            isCancelled = true
        }
    }
}
