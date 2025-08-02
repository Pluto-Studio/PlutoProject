package plutoproject.feature.paper.api.exchangeshop

import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.time.DayOfWeek

/**
 * 代表一个被加入到兑换商店中的商品。
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
     * 该商品的物品堆，返回一个不会影响内部物品堆的副本。
     */
    val itemStack: ItemStack

    /**
     * 购买一个数量单位的该商品所需的兑换券。
     */
    val ticketConsumption: Long

    /**
     * 一个数量单位的该商品的价格。
     */
    val price: BigDecimal

    /**
     * 该商品的数量单位。
     *
     * 每次交易会获得购买个数 * [quantity] 个物品堆。
     *
     * 例如：当购买个数为 2，[quantity] 为 4，则玩家实际获得 8 个物品堆。
     *
     * 需要注意的是 [itemStack] 本身的 amount 属性与此无关，在上面的例子中，如果 amount 为 2，则实际获得 16 个指定类型的物品。
     */
    val quantity: Int

    /**
     * 该商品的限期，若没有限期则为空。
     */
    val availableDays: Set<DayOfWeek>

    /**
     * 该商品是否在今日可购，若无限期则始终可购买。
     *
     * 该属性不受限期功能开关影响。
     */
    val isAvailableToday: Boolean

    /**
     * 检查该商品是否花费货币。
     */
    val hasMoneyCost: Boolean

    /**
     * 检查该商品是否消耗兑换券。
     */
    val hasTicketConsumption: Boolean

    /**
     * 检查该商品是否免费，即不需要花费货币和兑换券。
     */
    val isFree: Boolean

    /**
     * 检查该商品的数量单位是否为多个物品。
     */
    val isMultipleQuantity: Boolean

    /**
     * 检查该商品是否只花费货币，即无兑换券消耗。
     */
    val isMoneyOnly: Boolean

    /**
     * 检查该商品是否只花费兑换券，即无货币消耗。
     */
    val isTicketOnly: Boolean
}
