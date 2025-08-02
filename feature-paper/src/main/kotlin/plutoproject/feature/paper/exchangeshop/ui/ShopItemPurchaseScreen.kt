package plutoproject.feature.paper.exchangeshop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.exchangeshop.ShopItem
import plutoproject.feature.paper.exchangeshop.*
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.chat.component.replaceColor
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.canvas.dialog.Dialog
import plutoproject.framework.paper.api.interactive.canvas.dialog.body.ItemBody
import plutoproject.framework.paper.api.interactive.canvas.dialog.input.NumberRangeInput

@Suppress("UnstableApiUsage")
class ShopItemPurchaseScreen(
    private val shopItem: ShopItem,
    private val totalPurchasable: Long
) : InteractiveScreen() {
    private var amount = shopItem.quantity

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val backCallback = DialogAction.customClick(
            { _, _ -> navigator.pop() },
            ClickCallback.Options.builder().build()
        )
        val submitCallback = DialogAction.customClick({ view, _ ->
            val purchaseAmount = view.getFloat("amount")!!.toInt()
            amount = purchaseAmount
            navigator.push(ShopItemPurchaseConfirmationScreen(shopItem, purchaseAmount))
        }, ClickCallback.Options.builder().build())

        val backButton = ActionButton.builder(SHOP_ITEM_PURCHASE_BACK).action(backCallback).build()
        val submitButton = ActionButton.builder(SHOP_ITEM_PURCHASE_SUBMIT).action(submitCallback).build()

        Dialog(
            type = DialogType.confirmation(submitButton, backButton),
            canCloseWithEscape = false,
            title = SHOP_ITEM_PURCHASE_TITLE,
            body = {
                val itemStack = shopItem.itemStack
                val itemName = itemStack.getData(DataComponentTypes.ITEM_NAME)
                val customName = itemStack.getData(DataComponentTypes.CUSTOM_NAME)
                val name = customName ?: itemName ?: Component.translatable(itemStack.type.translationKey())
                val lore = itemStack.getData(DataComponentTypes.LORE)?.lines() ?: emptyList()

                val modifiedItemStack = itemStack.clone().apply {
                    setData(
                        DataComponentTypes.CUSTOM_NAME,
                        name.colorIfAbsent(mochaText)
                            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                    )
                    setData(DataComponentTypes.LORE, ItemLore.lore(lore.map { it.colorIfAbsent(mochaSubtext0) }))
                }

                ItemBody(
                    itemStack = modifiedItemStack,
                    description = DialogBody.plainMessage(SHOP_ITEM_PURCHASE_PURCHASING.replace("<item>", name), 1024)
                )

                ItemBody(
                    itemStack = ItemStack.of(Material.SUNFLOWER),
                    description = DialogBody.plainMessage(shopItem.priceDisplay.convertColors(), 1024),
                    showTooltip = false
                )
            },
            input = {
                NumberRangeInput(
                    key = "amount",
                    label = SHOP_ITEM_PURCHASE_QUANTITY,
                    start = shopItem.quantity.toFloat(),
                    end = totalPurchasable.coerceIn(shopItem.quantity.toLong(), 1000).toFloat(),
                    initial = amount.toFloat(),
                    step = shopItem.quantity.toFloat(),
                    width = 300,
                )
            }
        )
    }

    private fun Component.convertColors(): Component {
        return replaceColor(mochaText, mochaLavender).replaceColor(mochaSubtext0, mochaText)
    }
}
