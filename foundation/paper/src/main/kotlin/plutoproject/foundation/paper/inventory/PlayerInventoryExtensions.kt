package plutoproject.foundation.paper.inventory

import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

fun PlayerInventory.addItemOrDrop(vararg items: ItemStack): Map<Int, ItemStack> {
    val player = holder as? Player ?: error("Cannot get holder player")
    return addItem(*items).also { leftovers ->
        leftovers.values.forEach { stack ->
            player.world.createEntity(player.location, Item::class.java).also {
                it.itemStack = stack
                player.world.addEntity(it)
            }
        }
    }
}

val PlayerInventory.isFull: Boolean
    get() = !storageContents.contains(null)
