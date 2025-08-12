package plutoproject.feature.paper.exchangeshop.ui

import androidx.compose.runtime.Composable
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.exchangeshop.ShopTransaction
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.feature.paper.exchangeshop.*
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.time.format24Hour
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.layout.list.ListMenu
import java.time.ZoneId

@Suppress("UnstableApiUsage")
class TransactionHistoryScreen(
    private val shopUser: ShopUser
) : ListMenu<ShopTransaction, TransactionHistoryScreenModel>() {
    @Composable
    override fun MenuLayout() {
        LocalListMenuOptions.current.title = EXCHANGE_SHOP_TRANSACTION_HISTORY_TITLE
        super.MenuLayout()
    }

    @Composable
    override fun modelProvider(): TransactionHistoryScreenModel {
        return TransactionHistoryScreenModel(shopUser)
    }

    @Composable
    override fun Element(obj: ShopTransaction) {
        val time = obj.time.atZone(ZoneId.systemDefault()).format24Hour()
        val shopItemStack = if (obj.itemStack == null) ItemStack.of(Material.PAPER) else obj.itemStack!!

        val itemStack = shopItemStack.apply {
            val itemName = obj.itemStack?.getData(DataComponentTypes.ITEM_NAME)?.color(mochaText)
            val customName = obj.itemStack?.getData(DataComponentTypes.CUSTOM_NAME)?.color(mochaText)
            val name = TRANSACTION_HISTORY
                .replace("<name>", customName ?: itemName ?: TRANSACTION_HISTORY_UNKNOWN_ITEM)
            val originalLore = getData(DataComponentTypes.LORE)?.lines()

            val lore = buildList {
                if (!originalLore.isNullOrEmpty()) {
                    addAll(originalLore.map { it.colorIfAbsent(mochaText) })
                    add(Component.empty())
                }
                if (obj.itemStack == null) {
                    add(TRANSACTION_HISTORY_LORE_UNKNOWN_ITEM_DESC_1)
                    add(TRANSACTION_HISTORY_LORE_UNKNOWN_ITEM_DESC_2)
                    add(Component.empty())
                }
                add(TRANSACTION_HISTORY_TIME.replace("<time>", time))
                add(TRANSACTION_HISTORY_QUANTITY.replace("<quantity>", obj.quantity))
                add(TRANSACTION_HISTORY_COST.replace("<money>", obj.cost.stripTrailingZeros().toPlainString()))
                add(TRANSACTION_HISTORY_TICKET.replace("<ticket>", obj.ticket))
                add(TRANSACTION_HISTORY_BALANCE.replace("<balance>", obj.balance))
            }

            setData(DataComponentTypes.CUSTOM_NAME, name)
            setData(DataComponentTypes.LORE, ItemLore.lore(lore))
        }

        Item(itemStack = itemStack)
    }
}
