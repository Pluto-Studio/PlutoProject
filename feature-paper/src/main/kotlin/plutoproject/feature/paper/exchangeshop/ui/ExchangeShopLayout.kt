package plutoproject.feature.paper.exchangeshop.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.daily.screens.DailyCalenderScreen
import plutoproject.feature.paper.exchangeshop.*
import plutoproject.framework.common.api.databasepersist.adapters.BooleanTypeAdapter
import plutoproject.framework.common.util.chat.UI_TOGGLE_OFF_SOUND
import plutoproject.framework.common.util.chat.UI_TOGGLE_ON_SOUND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.paper.api.databasepersist.persistContainer
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.animations.spinnerAnimation
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.util.hook.vaultHook

private val config by Koin.inject<ExchangeShopConfig>()

@Composable
fun Availability(changeCallback: (Boolean) -> Unit = {}) {
    val player = LocalPlayer.current
    val container = remember(player) { player.persistContainer }
    val coroutineScope = rememberCoroutineScope()
    var showUnavailableItems: Boolean? by rememberSaveable { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        showUnavailableItems = container.getOrDefault(
            SHOW_UNAVAILABLE_ITEMS_PERSIST_KEY, BooleanTypeAdapter, SHOW_UNAVAILABLE_ITEMS_DEFAULT
        )
    }

    Item(
        material = Material.CLOCK,
        name = EXCHANGE_SHOP_AVAILABILITY,
        lore = buildList {
            if (showUnavailableItems == null) {
                add(EXCHANGE_SHOP_LOADING.replace("<spinner>", spinnerAnimation()))
            } else if (showUnavailableItems!!) {
                add(EXCHANGE_SHOP_AVAILABILITY_LORE_SHOW_UNAVAILABLE_ITEMS_ON)
            } else {
                add(EXCHANGE_SHOP_AVAILABILITY_LORE_SHOW_UNAVAILABLE_ITEMS_OFF)
            }

            add(Component.empty())
            add(EXCHANGE_SHOP_AVAILABILITY_LORE_DESC_1)
            add(EXCHANGE_SHOP_AVAILABILITY_LORE_DESC_2)
            add(EXCHANGE_SHOP_AVAILABILITY_LORE_DESC_3)
            if (showUnavailableItems == null) return@buildList
            add(Component.empty())

            if (showUnavailableItems!!) {
                add(EXCHANGE_SHOP_AVAILABILITY_LORE_OPERATION_HIDE_UNAVAILABLE_ITEMS)
            } else {
                add(EXCHANGE_SHOP_AVAILABILITY_LORE_OPERATION_SHOW_UNAVAILABLE_ITEMS)
            }
        },
        enchantmentGlint = showUnavailableItems == true,
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            if (showUnavailableItems == null) return@clickable
            val newState = !showUnavailableItems!!
            showUnavailableItems = newState
            changeCallback(newState)
            coroutineScope.launch {
                container.set(SHOW_UNAVAILABLE_ITEMS_PERSIST_KEY, BooleanTypeAdapter, newState)
                container.save()
            }
            if (newState) {
                player.playSound(UI_TOGGLE_ON_SOUND)
            } else {
                player.playSound(UI_TOGGLE_OFF_SOUND)
            }
        }
    )
}

@Composable
fun Economy() {
    val player = LocalPlayer.current
    val navigator = LocalNavigator.currentOrThrow
    val balance = vaultHook?.economy!!.getBalance(player).toBigDecimal()

    Item(
        material = Material.SUNFLOWER,
        name = EXCHANGE_SHOP_ECONOMY,
        lore = buildList {
            add(EXCHANGE_SHOP_ECONOMY_BALANCE.replace("<balance>", balance.stripTrailingZeros().toPlainString()))
            add(EXCHANGE_SHOP_ECONOMY_BALANCE_LORE_DESC)
            add(Component.empty())
            add(EXCHANGE_SHOP_ECONOMY_BALANCE_LORE_OPERATION)
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            navigator.push(DailyCalenderScreen())
        }
    )
}

@Composable
fun Ticket() {
    val amount = ticketAmount()
    Item(
        material = Material.PAPER,
        name = EXCHANGE_SHOP_TICKET,
        lore = buildList {
            add(
                EXCHANGE_SHOP_TICKET_LORE_TICKET
                    .replace("<ticket>", amount)
                    .replace("<cap>", config.ticket.recoveryCap),
            )
            add(ticketRecoveryIntervalDisplay())

            add(Component.empty())
            add(EXCHANGE_SHOP_TICKET_LORE_DESC_1)
            add(EXCHANGE_SHOP_TICKET_LORE_DESC_2)
            add(EXCHANGE_SHOP_TICKET_LORE_DESC_3)

            // 星币购买还没写
            // add(Component.empty())
            // add(EXCHANGE_SHOP_TICKET_LORE_OPERATION_BUY_TICKET.replace("<amount>", 1))
        }
    )
}

@Composable
fun Transaction() {
    val player = LocalPlayer.current
    val navigator = LocalNavigator.currentOrThrow
    var isLoading by remember { mutableStateOf(true) }
    var purchaseTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val user = ExchangeShop.getUserOrCreate(player)
        purchaseTime = user.countTransactions()
        isLoading = false
    }

    Item(
        material = Material.NAME_TAG,
        name = EXCHANGE_SHOP_TRANSACTION_HISTORY,
        lore = buildList {
            if (isLoading) {
                add(EXCHANGE_SHOP_LOADING.replace("<spinner>", spinnerAnimation()))
                return@buildList
            }
            if (purchaseTime == 0L) {
                add(EXCHANGE_SHOP_TRANSACTION_HISTORY_LORE_DESC_NO_PURCHASE)
            } else {
                add(EXCHANGE_SHOP_TRANSACTION_HISTORY_LORE_DESC.replace("<time>", purchaseTime))
            }
            add(Component.empty())
            add(EXCHANGE_SHOP_TRANSACTION_HISTORY_LORE_OPERATION)
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            navigator.push(TransactionHistoryScreen())
        }
    )
}
