package plutoproject.feature.paper.exchangeshop

import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopItem
import java.math.BigDecimal
import java.time.DayOfWeek

data class ShopItemImpl(
    override val id: String,
    override val category: ShopCategory,
    override val itemStack: ItemStack,
    override val ticketConsumption: Long,
    override val price: BigDecimal,
    override val quantity: Int,
    override val availableDays: List<DayOfWeek>
) : ShopItem
