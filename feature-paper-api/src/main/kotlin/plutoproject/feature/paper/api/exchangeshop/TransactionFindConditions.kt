package plutoproject.feature.paper.api.exchangeshop

import org.bukkit.Material
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * 代表进行交易记录查询时的条件。
 */
data class TransactionFindConditions(
    /**
     * 交易的 ID。
     */
    val id: UUID? = null,

    /**
     * 交易的时间周期。
     *
     * 若不设置则在该玩家的所有记录中查询。
     */
    val period: TimePeriod? = null,

    /**
     * 交易的物品类型。
     */
    val material: Material? = null,

    /**
     * 交易的购买数，表示玩家购买了多少个 [ShopItem.quantity] 单位的商品。
     */
    val amount: Int? = null,

    /**
     * 交易实际获得的物品堆数量。
     *
     * 每次交易会获得 [amount] * [ShopItem.quantity] 个物品堆。
     *
     * 例如：当 [amount] 为 2，[ShopItem.quantity] 为 4，则玩家实际获得 8 个物品堆。
     *
     * 需要注意的是 [ShopItem.itemStack] 本身的 amount 属性与此无关，在上面的例子中，如果 amount 为 2，则实际获得 16 个指定类型的物品。
     */
    val quantity: Int? = null,

    /**
     * 交易花费的兑换券。
     */
    val ticket: Int? = null,

    /**
     * 交易花费的货币。
     */
    val cost: BigDecimal? = null,

    /**
     * 交易的货币结余。
     */
    val balance: BigDecimal? = null,
)

/**
 * 代表查询的时间周期。
 */
data class TimePeriod(val startTime: Instant, val endTime: Instant)
