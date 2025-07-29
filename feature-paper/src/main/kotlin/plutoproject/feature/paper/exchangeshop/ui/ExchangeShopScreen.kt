package plutoproject.feature.paper.exchangeshop.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.ClickType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.exchangeshop.*
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.canvas.Menu
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.ItemSpacer
import plutoproject.framework.paper.api.interactive.jetpack.Arrangement
import plutoproject.framework.paper.api.interactive.layout.Column
import plutoproject.framework.paper.api.interactive.layout.Row
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxSize
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxWidth
import plutoproject.framework.paper.api.interactive.modifiers.height

class ExchangeShopScreen : InteractiveScreen(), KoinComponent {
    private val config by inject<ExchangeShopConfig>()

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
                val categoryId = config.layout.icons.firstOrNull { it.pattern == pattern }?.category
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
        val category = ExchangeShop.getCategory(id)
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
