package plutoproject.feature.paper.api.exchangeshop

import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * 代表一个被成功执行的兑换商店交易。
 */
interface ShopTransaction {
    /**
     * 交易的 ID。
     */
    val id: UUID

    /**
     * 参与该交易的玩家 UUID。
     */
    val playerId: UUID

    /**
     * 交易发生的时间。
     */
    val time: Instant

    /**
     * 交易所涉及的商品 ID。
     */
    val itemId: String

    /**
     * 交易所涉及的物品堆，与玩家获取到的物品堆一致。
     */
    val itemStack: ItemStack

    /**
     * 交易的购买数，表示玩家购买了多少个 [ShopItem.quantity] 单位的商品。
     */
    val amount: Int

    /**
     * 交易实际获得的物品堆数量。
     *
     * 每次交易会获得 [amount] * [ShopItem.quantity] 个物品堆。
     *
     * 例如：当 [amount] 为 2，[ShopItem.quantity] 为 4，则玩家实际获得 8 个物品堆。
     *
     * 需要注意的是 [ShopItem.itemStack] 本身的 amount 属性与此无关，在上面的例子中，如果 amount 为 2，则实际获得 16 个指定类型的物品。
     */
    val quantity: Int

    /**
     * 交易花费的兑换券。
     */
    val ticket: Int

    /**
     * 交易花费的货币。
     */
    val cost: BigDecimal

    /**
     * 交易的货币结余。
     */
    val balance: BigDecimal
}
