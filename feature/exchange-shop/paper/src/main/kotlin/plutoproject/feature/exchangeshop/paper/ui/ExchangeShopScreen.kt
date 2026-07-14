package plutoproject.feature.exchangeshop.paper.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.ClickType
import plutoproject.kernel.api.koinInject
import plutoproject.feature.exchangeshop.api.paper.ExchangeShop
import plutoproject.feature.exchangeshop.paper.*
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.capability.interactive.api.InteractiveScreen
import plutoproject.capability.interactive.api.canvas.Menu
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.components.ItemSpacer
import plutoproject.capability.interactive.api.jetpack.Arrangement
import plutoproject.capability.interactive.api.layout.Column
import plutoproject.capability.interactive.api.layout.Row
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.modifiers.fillMaxSize
import plutoproject.capability.interactive.api.modifiers.fillMaxWidth
import plutoproject.capability.interactive.api.modifiers.height

class ExchangeShopScreen : InteractiveScreen() {
    private val config by koinInject<ExchangeShopConfig>()

    @Composable
    override fun Content() {
        Menu(
            title = Component.text(EXCHANGE_SHOP_NAME),
            rows = config.layout.patterns.size + 2,
            bottomBorderAttachment = {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                    if (config.capability.availableDays) {
                        Availability()
                        ItemSpacer()
                    }
                    Economy()
                    ItemSpacer()
                    Ticket()
                    ItemSpacer()
                    Transaction()
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                config.layout.patterns.forEach { LayoutRow(it) }
            }
        }
    }

    @Composable
    private fun LayoutRow(row: String) {
        Row(modifier = Modifier.fillMaxWidth().height(1)) {
            row.forEach { pattern ->
                if (pattern.isWhitespace()) {
                    ItemSpacer()
                    return@forEach
                }
                val categoryId = config.layout.icons.firstOrNull { it.pattern.single() == pattern }?.category
                if (categoryId == null) {
                    featureLogger.warning("Unable to find a icon defined for pattern '$pattern'")
                    ItemSpacer()
                    return@forEach
                }
                Category(categoryId)
            }
        }
    }

    @Composable
    private fun Category(id: String) {
        val category = plutoproject.kernel.api.koinGet<ExchangeShop>().getCategory(id)
        val navigator = LocalNavigator.currentOrThrow

        if (category == null) {
            featureLogger.warning("Unable to find a category with id '$id'")
            ItemSpacer()
            return
        }

        Item(
            material = category.icon,
            name = category.name.colorIfAbsent(mochaText),
            lore = buildList {
                if (category.items.isNotEmpty()) {
                    add(EXCHANGE_SHOP_CATEGORY_SELECT_LORE_ITEMS.replace("<items>", category.items.size))
                }
                addAll(category.description.map { it.colorIfAbsent(mochaSubtext0) })
                add(Component.empty())
                add(EXCHANGE_SHOP_CATEGORY_SELECT_LORE_OPERATION)
            },
            modifier = Modifier.clickable {
                if (clickType != ClickType.LEFT) return@clickable
                navigator.push(ShopCategoryScreen(category))
            }
        )
    }
}
