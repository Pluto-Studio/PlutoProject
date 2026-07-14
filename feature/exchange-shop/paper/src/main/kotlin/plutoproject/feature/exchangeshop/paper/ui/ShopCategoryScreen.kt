package plutoproject.feature.exchangeshop.paper.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.raw
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.kernel.api.koinInject
import plutoproject.feature.exchangeshop.api.paper.ExchangeShop
import plutoproject.feature.exchangeshop.api.paper.ShopCategory
import plutoproject.feature.exchangeshop.api.paper.ShopItem
import plutoproject.feature.exchangeshop.paper.*
import plutoproject.feature.exchangeshop.paper.ui.ShopCategoryScreen.PageTurnerMode.NEXT
import plutoproject.feature.exchangeshop.paper.ui.ShopCategoryScreen.PageTurnerMode.PREVIOUS
import plutoproject.feature.exchangeshop.paper.ui.ShopCategoryScreen.ShopItemState.*
import plutoproject.foundation.common.text.UI_PAGING_SOUND
import plutoproject.foundation.common.text.replace
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.capability.interactive.api.InteractiveScreen
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.animations.loadingIconAnimation
import plutoproject.capability.interactive.api.animations.spinnerAnimation
import plutoproject.capability.interactive.api.canvas.Menu
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.components.ItemSpacer
import plutoproject.capability.interactive.api.jetpack.Arrangement
import plutoproject.capability.interactive.api.layout.Column
import plutoproject.capability.interactive.api.layout.Row
import plutoproject.capability.interactive.api.layout.VerticalGrid
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.modifiers.fillMaxSize
import plutoproject.capability.interactive.api.modifiers.fillMaxWidth
import plutoproject.capability.interactive.api.modifiers.height
import plutoproject.feature.exchangeshop.paper.server
import plutoproject.foundation.paper.hook.vaultEconomy
import java.time.format.TextStyle
import java.util.*

val LocalShopCategoryScreenModel: ProvidableCompositionLocal<ShopCategoryScreenModel> = staticCompositionLocalOf {
    error("Unexpected")
}

class ShopCategoryScreen(private val category: ShopCategory) : InteractiveScreen() {
    private val config by koinInject<ExchangeShopConfig>()

    @Composable
    override fun Content() {
        val player = LocalPlayer.current
        val screenModel = rememberScreenModel {
            ShopCategoryScreenModel(player, category)
        }

        LaunchedEffect(Unit) {
            screenModel.initialize()
        }

        CompositionLocalProvider(LocalShopCategoryScreenModel provides screenModel) {
            Layout()
        }
    }

    @Composable
    private fun Layout() {
        val screenModel = LocalShopCategoryScreenModel.current
        Menu(
            title = category.name.color(null),
            rows = 6,
            bottomBorderAttachment = {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                    if (screenModel.isLoading) return@Row
                    if (config.capability.availableDays) {
                        AvailabilityWithCallback()
                        ItemSpacer()
                    } else {
                        Economy()
                        ItemSpacer()
                    }
                    if (screenModel.totalPages > 1) {
                        PageTurner(PREVIOUS)
                        ItemSpacer()
                        PageTurner(NEXT)
                        ItemSpacer()
                        Ticket()
                    } else {
                        Ticket()
                        ItemSpacer()
                        Transaction()
                    }
                }
            }
        ) {
            if (screenModel.isLoading) {
                Loading()
                return@Menu
            }

            Contents()
        }
    }

    @Composable
    private fun AvailabilityWithCallback() {
        val screenModel = LocalShopCategoryScreenModel.current
        Availability(changeCallback = {
            screenModel.showUnavailableItems = it
            screenModel.loadPageInitially()
        })
    }

    private enum class PageTurnerMode {
        PREVIOUS, NEXT
    }

    @Composable
    private fun PageTurner(mode: PageTurnerMode) {
        val screenModel = LocalShopCategoryScreenModel.current
        Item(
            material = Material.ARROW,
            name = CATEGORY_PAGE_INDICATOR
                .replace("<currentPage>", screenModel.currentPage + 1)
                .replace("<totalPages>", screenModel.totalPages),
            lore = buildList {
                if (mode == PREVIOUS && screenModel.currentPage == 0) return@buildList
                if (mode == NEXT && screenModel.currentPage == screenModel.totalPages - 1) return@buildList

                add(Component.empty())
                when (mode) {
                    PREVIOUS -> add(CATEGORY_PREVIOUS_PAGE_LORE_OPERATION)
                    NEXT -> add(CATEGORY_NEXT_PAGE_LORE_OPERATION)
                }
            },
            modifier = Modifier.clickable {
                if (clickType != ClickType.LEFT) return@clickable

                when (mode) {
                    PREVIOUS -> {
                        if (screenModel.currentPage == 0) return@clickable
                        screenModel.goPreviousPage()
                    }

                    NEXT -> {
                        if (screenModel.currentPage == screenModel.totalPages - 1) return@clickable
                        screenModel.goNextPage()
                    }
                }

                whoClicked.playSound(UI_PAGING_SOUND)
            }
        )
    }

    @Composable
    private fun Loading() {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Row(modifier = Modifier.fillMaxWidth().height(2), horizontalArrangement = Arrangement.Center) {
                Item(
                    material = loadingIconAnimation(),
                    name = Component.text("${spinnerAnimation()} 正在加载...").color(mochaSubtext0)
                )
            }
        }
    }

    @Composable
    private fun Contents() {
        val screenModel = LocalShopCategoryScreenModel.current
        VerticalGrid(modifier = Modifier.fillMaxSize()) {
            screenModel.pageContents.forEach { ShopItem(it) }
        }
    }

    private enum class ShopItemState {
        NORMAL, UNAVAILABLE, BALANCE_NOT_ENOUGH, TICKET_NOT_ENOUGH
    }

    @Composable
    @Suppress("UnstableApiUsage")
    private fun ShopItem(shopItem: ShopItem) {
        val player = LocalPlayer.current
        val navigator = LocalNavigator.currentOrThrow

        val economy = server.vaultEconomy!!
        val ticket = ticketAmount()
        val balance = economy.getBalance(player).toBigDecimal()
        var purchasableQuantity: Long? by remember(shopItem) { mutableStateOf(null) }

        LaunchedEffect(shopItem) {
            val user = plutoproject.kernel.api.koinGet<ExchangeShop>().getUserOrCreate(player)
            purchasableQuantity = user.calculatePurchasableQuantity(shopItem)
        }

        val state by remember(shopItem, shopItem.isAvailableToday, ticket, balance) {
            mutableStateOf(
                when {
                    config.capability.availableDays && !shopItem.isAvailableToday -> UNAVAILABLE
                    balance < shopItem.price -> BALANCE_NOT_ENOUGH
                    ticket < shopItem.ticketConsumption -> TICKET_NOT_ENOUGH
                    else -> NORMAL
                }
            )
        }

        if (purchasableQuantity == null) {
            val itemStack = shopItem.itemStack.apply {
                setData(DataComponentTypes.CUSTOM_NAME, EXCHANGE_SHOP_LOADING.replace("<spinner>", spinnerAnimation()))
                setData(DataComponentTypes.LORE, ItemLore.lore(emptyList()))
            }
            Item(itemStack = itemStack)
            return
        }

        val itemStack = shopItem.itemStack.apply {
            val itemName = getData(DataComponentTypes.ITEM_NAME)
            val customName = getData(DataComponentTypes.CUSTOM_NAME)
            val name = itemName ?: customName ?: Component.translatable(type.translationKey())

            val originalLore = getData(DataComponentTypes.LORE)?.lines()
            val lore = buildList {
                if (!originalLore.isNullOrEmpty()) {
                    addAll(originalLore.map { it.colorIfAbsent(mochaSubtext0) })
                    add(Component.empty())
                }

                if (config.capability.availableDays && shopItem.availableDays.isNotEmpty()) {
                    val availableDays = component {
                        raw(SHOP_ITEM_LORE_AVAILABLE_DAYS)
                        val sortedAvailableDays = shopItem.availableDays.sorted()
                        sortedAvailableDays.forEachIndexed { index, dayOfWeek ->
                            val displayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.SIMPLIFIED_CHINESE)
                            raw(SHOP_ITEM_LORE_AVAILABLE_DAYS_DAY_OF_WEEK.replace("<day>", displayName))
                            if (index == sortedAvailableDays.indices.last) return@forEachIndexed
                            raw(SHOP_ITEM_LORE_AVAILABLE_DAYS_SEPARATOR)
                        }
                    }
                    add(availableDays)
                    add(Component.empty())
                }

                add(shopItem.priceDisplay)

                when (state) {
                    NORMAL -> if (!shopItem.isFree) {
                        add(SHOP_ITEM_LORE_PURCHASABLE_QUANTITY.replace("<quantity>", purchasableQuantity!!))
                    }

                    UNAVAILABLE -> {
                        add(SHOP_ITEM_LORE_UNAVAILABLE)
                        return@buildList
                    }

                    BALANCE_NOT_ENOUGH -> {
                        add(SHOP_ITEM_LORE_BALANCE_NOT_ENOUGH)
                        return@buildList
                    }

                    TICKET_NOT_ENOUGH -> {
                        add(SHOP_ITEM_LORE_TICKET_NOT_ENOUGH)
                        return@buildList
                    }
                }

                add(Component.empty())
                add(SHOP_ITEM_LORE_OPERATION)
            }

            setData(DataComponentTypes.CUSTOM_NAME, name.colorIfAbsent(mochaText))
            setData(DataComponentTypes.LORE, ItemLore.lore(lore))
        }

        Item(
            itemStack = itemStack,
            modifier = Modifier.clickable {
                if (clickType != ClickType.LEFT) return@clickable
                if (state != NORMAL) return@clickable
                navigator.push(ShopItemPurchaseScreen(shopItem, purchasableQuantity!!))
            }
        )
    }
}
