package plutoproject.framework.paper.api.interactive.canvas

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import org.bukkit.inventory.ItemStack
import plutoproject.framework.paper.api.interactive.util.IntCoordinates

open class MapCanvas : Canvas {
    private val contents = Long2ObjectOpenHashMap<ItemStack>()

    override fun set(x: Int, y: Int, item: ItemStack?) {
        if (item == null) contents.remove(IntCoordinates(x, y).pair)
        else contents[IntCoordinates(x, y).pair] = item
    }

    fun startRender() {
        contents.clear()
    }

    fun getCoordinates(): Map<IntCoordinates, ItemStack> {
        return contents.toMap().mapKeys { IntCoordinates(it.key) }
    }
}