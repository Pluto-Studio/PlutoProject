package plutoproject.feature.paper.exchangeshop.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.raw
import ink.pmc.advkt.component.replace
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopItem
import plutoproject.feature.paper.api.exchangeshop.ShopTransactionException
import plutoproject.feature.paper.exchangeshop.*
import plutoproject.feature.paper.exchangeshop.ui.PurchaseState.*
import plutoproject.framework.common.util.chat.UI_FAILED_SOUND
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.canvas.dialog.Dialog
import plutoproject.framework.paper.api.interactive.canvas.dialog.body.ItemBody
import plutoproject.framework.paper.api.interactive.canvas.dialog.body.PlainMessageBody
import kotlin.time.Duration.Companion.seconds

private enum class PurchaseState {
    NORMAL, SUCCEED, BALANCE_NOT_ENOUGH, TICKET_NOT_ENOUGH, SHOP_ITEM_NOT_AVAILABLE, DATABASE_FAILURE
}

@Suppress("UnstableApiUsage")
class ShopItemPurchaseConfirmationScreen(
    private val shopItem: ShopItem,
    private val purchaseAmount: Int
) : InteractiveScreen() {
    @Composable
    override fun Content() {
        val player = LocalPlayer.current
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()
        val amount = purchaseAmount / shopItem.quantity
        var state by remember { mutableStateOf(NORMAL) }

        fun stateTransition(newState: PurchaseState, pop: Boolean = false) {
            coroutineScope.launch {
                val keep = state
                state = newState
                delay(1.seconds)
                if (!pop) state = keep
                if (pop && newState == SUCCEED) {
                    navigator.popUntil { it is ShopCategoryScreen }
                } else if (pop) {
                    navigator.pop()
                }
            }
        }

        val cancelCallback = DialogAction.customClick(
            { _, _ -> navigator.pop() },
            ClickCallback.Options.builder().build()
        )
        val confirmCallback = DialogAction.customClick({ _, _ ->
            if (state != NORMAL) return@customClick
            coroutineScope.launch {
                val user = ExchangeShop.getUserOrCreate(player)
                val transactionError = user.makeTransaction(shopItem, amount).exceptionOrNull()

                val errorState = when (transactionError) {
                    is ShopTransactionException.BalanceNotEnough -> BALANCE_NOT_ENOUGH
                    is ShopTransactionException.TicketNotEnough -> TICKET_NOT_ENOUGH
                    is ShopTransactionException.ShopItemNotAvailable -> SHOP_ITEM_NOT_AVAILABLE
                    is ShopTransactionException.DatabaseFailure -> DATABASE_FAILURE
                    null -> null
                    else -> error("Unexpected")
                }

                if (errorState != null) {
                    player.playSound(UI_FAILED_SOUND)
                    stateTransition(errorState)
                    return@launch
                }

                // 目前的 Dialog 框架问题太多，状态刷新有严重问题
                // 所以直接把菜单关了
                player.playSound(UI_SUCCEED_SOUND)
                if (navigator.items.any { it is ShopCategoryScreen }) {
                    navigator.popUntil { it is ShopCategoryScreen }
                } else {
                    navigator.pop()
                }
            }
        }, ClickCallback.Options.builder().build())

        val cancelButton = ActionButton
            .builder(SHOP_ITEM_PURCHASE_CONFIRMATION_CANCEL)
            .action(cancelCallback)
            .build()
        val confirmButton = ActionButton
            .builder(SHOP_ITEM_PURCHASE_CONFIRMATION_CONFIRM)
            .action(confirmCallback)
            .build()

        Dialog(
            type = DialogType.confirmation(confirmButton, cancelButton),
            canCloseWithEscape = false,
            title = SHOP_ITEM_PURCHASE_CONFIRMATION_TITLE,
            body = {
                val cost = shopItem.price * amount.toBigDecimal()
                val ticketConsumption = shopItem.ticketConsumption * amount

                val itemStack = shopItem.itemStack
                val itemName = itemStack.getData(DataComponentTypes.ITEM_NAME)
                val customName = itemStack.getData(DataComponentTypes.CUSTOM_NAME)
                val name = customName ?: itemName ?: Component.translatable(itemStack.type.translationKey())

                val message = when (state) {
                    NORMAL -> null
                    SUCCEED -> SHOP_ITEM_PURCHASE_CONFIRMATION_SUCCEED
                    BALANCE_NOT_ENOUGH -> SHOP_ITEM_PURCHASE_CONFIRMATION_BALANCE_NOT_ENOUGH
                    TICKET_NOT_ENOUGH -> SHOP_ITEM_PURCHASE_CONFIRMATION_TICKET_NOT_ENOUGH
                    SHOP_ITEM_NOT_AVAILABLE -> SHOP_ITEM_PURCHASE_CONFIRMATION_ITEM_NOT_AVAILABLE
                    DATABASE_FAILURE -> SHOP_ITEM_PURCHASE_CONFIRMATION_DATABASE_FAILURE
                }

                if (message != null) {
                    PlainMessageBody(message)
                    return@Dialog
                }

                ItemBody(
                    itemStack = ItemStack.of(Material.OAK_SIGN),
                    description = DialogBody.plainMessage(
                        SHOP_ITEM_PURCHASE_CONFIRMATION
                            .replace("<item>", name)
                            .replace("<quantity>", Component.text(this.purchaseAmount))
                    ),
                    showTooltip = false
                )

                if (!shopItem.isFree) {
                    val costDisplay = component {
                        raw(SHOP_ITEM_PURCHASE_CONFIRMATION_PRICE)
                        if (shopItem.hasMoneyCost) {
                            raw(
                                SHOP_ITEM_PURCHASE_CONFIRMATION_PRICE_MONEY
                                    .replace("<cost>", Component.text(cost.stripTrailingZeros().toPlainString()))
                            )
                        }
                        if (shopItem.hasMoneyCost && shopItem.hasTicketConsumption) {
                            raw(SHOP_ITEM_PURCHASE_CONFIRMATION_PRICE_AND)
                        }
                        if (shopItem.hasTicketConsumption) {
                            raw(
                                SHOP_ITEM_PURCHASE_CONFIRMATION_PRICE_TICKET
                                    .replace("<ticket>", Component.text(ticketConsumption))
                            )
                        }
                    }

                    ItemBody(
                        itemStack = ItemStack.of(Material.SUNFLOWER),
                        description = DialogBody.plainMessage(costDisplay),
                        showTooltip = false
                    )
                }
            }
        )
    }
}
