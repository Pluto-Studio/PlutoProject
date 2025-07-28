package plutoproject.feature.paper.exchangeshop

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopItem
import java.math.BigDecimal
import java.time.DayOfWeek

class ShopCategoryImpl(
    override val id: String,
    override val icon: Material,
    override val name: Component,
    override val description: List<Component>
) : ShopCategory {
    private val internalItems = linkedMapOf<String, ShopItem>()

    override val items: List<ShopItem>
        get() = internalItems.values.toList()

    override fun addItem(
        id: String,
        itemStack: ItemStack,
        ticketConsumption: Long,
        price: BigDecimal,
        quantity: Int,
        availableDays: List<DayOfWeek>
    ): ShopItem {
        require(id.isValidIdentifier()) { "ID must contain only English letters, numbers and underscores: $id" }
        require(!hasItem(id)) { "Shop item with ID `$id` already exists in category ${this.id}" }

        val item = ShopItemImpl(
            id = id,
            category = this,
            itemStack = itemStack,
            ticketConsumption = ticketConsumption,
            price = price,
            quantity = quantity,
            availableDays = availableDays
        )

        internalItems[id] = item
        return item
    }

    override fun getItem(id: String): ShopItem? {
        return internalItems[id]
    }

    override fun hasItem(id: String): Boolean {
        return internalItems.containsKey(id)
    }

    override fun removeItem(id: String): ShopItem? {
        return internalItems.remove(id)
    }
}
