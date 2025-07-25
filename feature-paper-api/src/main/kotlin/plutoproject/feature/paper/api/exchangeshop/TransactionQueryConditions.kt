package plutoproject.feature.paper.api.exchangeshop

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * 代表进行交易记录查询时的条件。
 */
data class TransactionQueryConditions(
    /**
     * 需要查询的交易 ID。
     */
    val id: UUID? = null,

    /**
     * 需要查询的时间周期。
     *
     * 若不设置则在该玩家的所有记录中查询。
     */
    val period: TimePeriod? = null,

    /**
     * 需要查询的物品类型，若设置 [item] 则需与其一致。
     */
    val itemType: Material? = null,

    /**
     * 需要查询的物品堆。
     *
     * 需要与玩家购买得到的物品堆一致，若设置 [itemType] 则该物品的类型需与其一致。
     */
    val item: ItemStack? = null,

    /**
     * 需要查询的兑换券花费。
     */
    val ticket: Int? = null,

    /**
     * 需要查询的货币花费。
     */
    val cost: BigDecimal? = null,

    /**
     * 需要查询的货币结余。
     */
    val balance: BigDecimal? = null,

    /**
     * 需要查询的购买个数。
     */
    val count: Int? = null,
) {
    init {
        if (itemType != null && item != null) {
            require(itemType == item.type) { "Expected item type $itemType, but got ${item.type}" }
        }
    }
}

/**
 * 代表查询的时间周期。
 */
data class TimePeriod(val startTime: Instant, val endTime: Instant)
