package plutoproject.feature.paper.exchangeshop.ui

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.raw
import ink.pmc.advkt.component.text
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.feature.paper.exchangeshop.*
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.paper.api.interactive.components.Item
import java.time.Duration

val ExchangeShopButtonDescriptor = ButtonDescriptor {
    id = "exchangeshop:shop"
}

private val config by Koin.inject<ExchangeShopConfig>()

@Composable
@Suppress("UnstableApiUsage")
fun ExchangeShop() {
    val itemStack = ItemStack.of(Material.BUNDLE).apply {
        setData(
            DataComponentTypes.TOOLTIP_DISPLAY,
            TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.BUNDLE_CONTENTS)
        )
        setData(DataComponentTypes.ITEM_NAME, EXCHANGE_SHOP_BUTTON)

        val lore = buildList {
            val amount = ticketAmount()
            val recoveryInterval = ticketRecoveryInterval()

            add(
                EXCHANGE_SHOP_BUTTON_TICKET
                    .replace("<ticket>", amount)
                    .replace("<cap>", config.ticket.recoveryCap)
            )
            if (amount < config.ticket.recoveryCap && recoveryInterval != null) {
                val intervalDisplay = Duration.ofSeconds(recoveryInterval.seconds + 1).toMMSSFormat()
                add(EXCHANGE_SHOP_BUTTON_TICKET_RECOVERY_INTERVAL.replace("<interval>", intervalDisplay))
            } else {
                add(EXCHANGE_SHOP_BUTTON_TICKET_FULL)
            }

            add(Component.empty())
            add(EXCHANGE_SHOP_BUTTON_DESCRIPTION_LINE_1)
            add(EXCHANGE_SHOP_BUTTON_DESCRIPTION_LINE_2)
            add(Component.empty())
            add(EXCHANGE_SHOP_BUTTON_OPERATION)
        }

        setData(DataComponentTypes.LORE, ItemLore.lore(lore))
    }

    Item(
        itemStack = itemStack
    )
}

fun Duration.toMMSSFormat(): String {
    val totalSeconds = this.seconds
    val minutes = (totalSeconds / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return String.format("%02d:%02d", minutes, seconds)
}
