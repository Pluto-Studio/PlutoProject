package plutoproject.capability.interactive.api.canvas

import org.bukkit.inventory.ItemStack

interface Canvas {
    fun set(x: Int, y: Int, item: ItemStack?)
}
