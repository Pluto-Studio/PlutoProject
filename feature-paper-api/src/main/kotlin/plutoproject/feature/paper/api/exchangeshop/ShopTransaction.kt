package plutoproject.feature.paper.api.exchangeshop

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * 代表一个被成功执行的兑换商店交易。
 */
interface ShopTransaction {
    /**
     * 该交易的 ID。
     */
    val id: UUID

    /**
     * 参与该交易的玩家 UUID。
     */
    val playerId: UUID

    /**
     * 该交易发生的时间。
     */
    val time: Instant

    /**
     * 该交易所涉及的物品类型。
     */
    val itemType: Material

    /**
     * 该交易所涉及的物品堆，与玩家获取到的物品堆一致。
     */
    val item: ItemStack

    /**
     * 该交易花费的兑换券。
     */
    val ticket: Int

    /**
     * 该交易花费的货币。
     */
    val cost: BigDecimal

    /**
     * 该交易的货币结余。
     */
    val balance: BigDecimal

    /**
     * 该交易的个数。
     */
    val count: Int
}
