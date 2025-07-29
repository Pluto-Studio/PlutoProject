package plutoproject.feature.paper.exchangeshop

import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopItem
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDateTime

class ShopItemImpl(
    override val id: String,
    override val category: ShopCategory,
    itemStack: ItemStack,
    override val ticketConsumption: Long,
    override val price: BigDecimal,
    override val quantity: Int,
    override val availableDays: Set<DayOfWeek>
) : ShopItem {
    private val _itemStack = itemStack
    override val itemStack: ItemStack
        get() = _itemStack.clone()
    override val isAvailableToday: Boolean
        get() = availableDays.isEmpty() || LocalDateTime.now().dayOfWeek in availableDays
    override val hasMoneyCost: Boolean = price > BigDecimal.ZERO
    override val hasTicketConsumption: Boolean = ticketConsumption > 0L
    override val isMoneyOnly: Boolean = hasMoneyCost && !hasTicketConsumption
    override val isTicketOnly: Boolean = !hasMoneyCost && hasTicketConsumption
    override val isFree: Boolean = !hasMoneyCost && !hasTicketConsumption
    override val isMultipleQuantity: Boolean = quantity > 1

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ShopItem
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
