package plutoproject.feature.paper.exchangeshop

import net.kyori.adventure.text.Component
import org.bukkit.Material
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Duration

data class ExchangeShopConfig(
    val capability: CapabilityConfig = CapabilityConfig(),
    val ticket: TicketConfig = TicketConfig(),
    val layout: LayoutConfig = LayoutConfig(),
    val categories: List<ShopCategoryConfig> = emptyList(),
    val items: List<ShopItemConfig> = emptyList(),
)

data class CapabilityConfig(
    val availableDays: Boolean = false,
)

data class TicketConfig(
    val naturalRecovery: Boolean = true,
    val recoveryInterval: Duration = Duration.ofMinutes(4),
    val recoveryAmount: Long = 1,
    val recoveryCap: Long = 320,
) {
    init {
        check(!recoveryInterval.isZero) { "Recovery interval cannot be zero" }
    }
}

data class LayoutConfig(
    val patterns: List<String> = emptyList(),
    val icons: List<LayoutIconConfig> = emptyList(),
) {
    init {
        check(patterns.size in 0..4) { "Pattern rows must be in [0, 4]" }
        check(patterns.all { it.length == 7 }) { "Each pattern row must have 7 items" }
    }
}

data class LayoutIconConfig(
    val pattern: Char,
    val category: String,
)

data class ShopCategoryConfig(
    val id: String,
    val icon: Material = Material.PAPER,
    val name: Component = Component.empty(),
    val description: List<Component> = emptyList(),
)

data class ShopItemConfig(
    val id: String,
    val category: String,
    val material: Material,
    val ticketConsumption: Long = 1,
    val price: BigDecimal = BigDecimal(1.0),
    val quantity: Int = 1,
    val availableDays: Set<DayOfWeek> = emptySet(),
) {
    init {
        check(price >= BigDecimal.ZERO) { "Price cannot be negative for shop item '$id'" }
        check(ticketConsumption >= 0) { "Ticket consumption cannot be negative for shop item '$id'" }
        check(quantity >= 1) { "Quantity must be at least 1 for shop item '$id'" }
    }
}
