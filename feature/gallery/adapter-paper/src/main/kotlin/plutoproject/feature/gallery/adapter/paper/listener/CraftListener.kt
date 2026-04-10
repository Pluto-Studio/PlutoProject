package plutoproject.feature.gallery.adapter.paper.listener

import org.bukkit.block.Crafter
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import plutoproject.feature.gallery.adapter.paper.isImageItem

@Suppress("UNUSED")
object CraftListener : Listener {
    @EventHandler
    fun onCraftItem(event: CraftItemEvent) {
        val ingredients = event.inventory.matrix.map { it ?: ItemStack.empty() }
        if (ingredients.any { it.isImageItem() }) {
            event.inventory.result = ItemStack.empty()
        }
    }

    @EventHandler
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        val ingredients = event.inventory.matrix.map { it ?: ItemStack.empty() }
        if (ingredients.any { it.isImageItem() }) {
            event.inventory.result = ItemStack.empty()
        }
    }

    @EventHandler
    fun onCrafterCraft(event: CrafterCraftEvent) {
        val crafter = event.block.state as? Crafter ?: return
        val ingredients = crafter.inventory.map { it ?: ItemStack.empty() }
        if (ingredients.any { it.isImageItem() }) {
            event.isCancelled = true
        }
    }
}
