package plutoproject.feature.paper.api.exchangeshop

import org.bukkit.Material
import java.math.BigDecimal
import java.time.DayOfWeek

/**
 * 代表一个被加入到兑换商店中的物品。
 */
interface ShopItem {
    /**
     * 该商品的 ID，需唯一。
     */
    val id: String

    /**
     * 该商品所属的类别。
     */
    val category: ShopCategory

    /**
     * 该商品的物品类型。
     */
    val material: Material

    /**
     * 兑换一次该商品消耗的兑换券。
     */
    val ticketConsumption: Int

    /**
     * 该商品的价格。
     */
    val price: BigDecimal

    /**
     * 兑换一次该商品获得的个数。
     */
    val count: Int

    /**
     * 该商品的限期，若没有限期则为空。
     */
    val availableDays: List<DayOfWeek>
}
