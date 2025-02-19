package plutoproject.framework.paper.util.inventory

import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

fun PlayerInventory.addItemOrDrop(vararg items: ItemStack): Map<Int, ItemStack> {
    val player = holder as? Player? ?: error("Cannot get holder player" )
    val left = addItem(*items)
    val location = player.location
    val world = player.world
    left.values.forEach {
        val entity = world.createEntity(location, Item::class.java)
        entity.itemStack = it
        world.addEntity(entity)
    }
    return left
}

val PlayerInventory.isFull: Boolean
    get() = !storageContents.contains(null)
