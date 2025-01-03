package plutoproject.framework.paper.api.interactive.click

import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack

data class ClickScope(
    val view: InventoryView,
    val clickType: ClickType,
    val slot: Int,
    val cursor: ItemStack?,
    val whoClicked: HumanEntity
)
