package plutoproject.feature.paper.exchangeshop.ui

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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopItem
import plutoproject.feature.paper.exchangeshop.*
import plutoproject.feature.paper.exchangeshop.ui.ShopCategoryScreen.PageTurnerMode.NEXT
import plutoproject.feature.paper.exchangeshop.ui.ShopCategoryScreen.PageTurnerMode.PREVIOUS
import plutoproject.feature.paper.exchangeshop.ui.ShopCategoryScreen.ShopItemState.*
import plutoproject.framework.common.util.chat.UI_PAGING_SOUND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.animations.loadingIconAnimation
import plutoproject.framework.paper.api.interactive.animations.spinnerAnimation
import plutoproject.framework.paper.api.interactive.canvas.Menu
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.ItemSpacer
import plutoproject.framework.paper.api.interactive.jetpack.Arrangement
import plutoproject.framework.paper.api.interactive.layout.Column
import plutoproject.framework.paper.api.interactive.layout.Row
import plutoproject.framework.paper.api.interactive.layout.VerticalGrid
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxSize
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxWidth
import plutoproject.framework.paper.api.interactive.modifiers.height
import plutoproject.framework.paper.util.hook.vaultHook
import java.time.format.TextStyle
import java.util.*

val LocalShopCategoryScreenModel: ProvidableCompositionLocal<ShopCategoryScreenModel> = staticCompositionLocalOf {
    error("Unexpected")
}

class ShopCategoryScreen(private val category: ShopCategory) : InteractiveScreen(), KoinComponent {
    private val config by inject<ExchangeShopConfig>()

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

        val economy = vaultHook?.economy!!
        val ticket = ticketAmount()
        val balance = economy.getBalance(player).toBigDecimal()
        var purchasableQuantity: Long? by remember(shopItem) { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            val user = ExchangeShop.getUserOrCreate(player)
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
