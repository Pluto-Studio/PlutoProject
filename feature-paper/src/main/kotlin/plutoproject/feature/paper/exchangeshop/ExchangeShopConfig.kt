package plutoproject.feature.paper.exchangeshop

import net.kyori.adventure.text.Component
import org.bukkit.Material
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Duration

data class ExchangeShopConfig(
    val ticket: TicketConfig = TicketConfig(),
    val layout: LayoutConfig = LayoutConfig(),
    val categories: List<ShopCategoryConfig> = emptyList(),
    val items: List<ShopItemConfig> = emptyList(),
)

data class TicketConfig(
    val naturalRecovery: Boolean = true,
    val recoveryInterval: Duration = Duration.ofMinutes(4),
    val recoveryAmount: Int = 1,
    val recoveryCap: Int = 320,
)

data class LayoutConfig(
    val patterns: List<String> = listOf("       ", "   E   ", "       "),
    val icons: List<LayoutIconConfig> = listOf(LayoutIconConfig("E")),
)

data class LayoutIconConfig(
    val pattern: String,
    val icon: Material = Material.PAPER,
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
    val ticketConsumption: Int = 1,
    val price: BigDecimal = BigDecimal(1.0),
    val count: Int = 1,
    val availableDays: List<DayOfWeek> = emptyList(),
)
