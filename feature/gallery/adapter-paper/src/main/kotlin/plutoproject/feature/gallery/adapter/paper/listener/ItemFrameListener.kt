package plutoproject.feature.gallery.adapter.paper.listener

import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent.ItemFrameChangeAction.*
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import plutoproject.feature.gallery.adapter.paper.IMAGE_ITEM_MATERIAL
import plutoproject.feature.gallery.adapter.paper.imageItemData

object ItemFrameListener : Listener {
    @EventHandler
    suspend fun onItemFrameChange(event: PlayerItemFrameChangeEvent) {
        if (event.itemStack.type != IMAGE_ITEM_MATERIAL
            || event.itemStack.imageItemData() == null
            || event.action == ROTATE
        ) {
            return
        }

        event.isCancelled = true

        when (event.action) {
            PLACE -> onPlace(event.itemFrame, event.itemStack)
            REMOVE -> onRemove(event.itemFrame, event.itemStack)
            ROTATE -> return
        }
    }

    private fun onPlace(itemFrame: ItemFrame, itemStack: ItemStack) {
        val imageItemData = itemStack.imageItemData() ?: return

    }

    private fun onRemove(itemFrame: ItemFrame, itemStack: ItemStack) {

    }
}
