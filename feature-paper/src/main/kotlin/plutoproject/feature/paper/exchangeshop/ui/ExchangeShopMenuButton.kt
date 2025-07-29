package plutoproject.feature.paper.exchangeshop.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.feature.paper.exchangeshop.*
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

val ExchangeShopButtonDescriptor = ButtonDescriptor {
    id = "exchangeshop:shop"
}

private val config by Koin.inject<ExchangeShopConfig>()

@Composable
@Suppress("UnstableApiUsage")
fun ExchangeShop() {
    val navigator = LocalNavigator.currentOrThrow
    val itemStack = ItemStack.of(Material.BUNDLE).apply {
        setData(
            DataComponentTypes.TOOLTIP_DISPLAY,
            TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.BUNDLE_CONTENTS)
        )
        setData(DataComponentTypes.ITEM_NAME, EXCHANGE_SHOP_BUTTON)

        val lore = buildList {
            val amount = ticketAmount()
            add(
                EXCHANGE_SHOP_BUTTON_LORE_TICKET
                    .replace("<ticket>", amount)
                    .replace("<cap>", config.ticket.recoveryCap)
            )
            add(ticketRecoveryIntervalDisplay())

            add(Component.empty())
            add(EXCHANGE_SHOP_BUTTON_LORE_DESC_LINE_1)
            add(EXCHANGE_SHOP_BUTTON_LORE_DESC_LINE_2)
            add(Component.empty())
            add(EXCHANGE_SHOP_BUTTON_LORE_OPERATION)
        }

        setData(DataComponentTypes.LORE, ItemLore.lore(lore))
    }

    Item(
        itemStack = itemStack,
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            navigator.push(ExchangeShopScreen())
        }
    )
}
