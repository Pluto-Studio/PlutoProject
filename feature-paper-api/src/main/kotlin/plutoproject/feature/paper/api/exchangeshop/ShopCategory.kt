package plutoproject.feature.paper.api.exchangeshop

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.time.DayOfWeek

/**
 * 代表一个兑换商店中的类别。
 */
interface ShopCategory {
    /**
     * 该类别的 ID，需唯一。
     */
    val id: String

    /**
     * 该类别在菜单中显示的图标。
     */
    val icon: Material

    /**
     * 该类别在菜单中显示的名称。
     */
    val name: Component

    /**
     * 该类别在菜单中显示的介绍。
     */
    val description: List<Component>

    /**
     * 该类别包含的商品。
     */
    val items: List<ShopItem>

    /**
     * 往该类别里添加一个商品。
     *
     * @param id 要添加的商品 ID
     * @param itemStack 商品的物品堆
     * @param ticketConsumption 购买一个数量单位的该商品所需的兑换券
     * @param price 一个数量单位的该商品的价格
     * @param quantity 商品的数量单位
     * @param availableDays 商品的限期，若列表为空则不限期
     * @throws IllegalArgumentException 同 ID 的商品已存在或 ID 不合法
     */
    fun addItem(
        id: String,
        itemStack: ItemStack,
        ticketConsumption: Int = 1,
        price: BigDecimal,
        quantity: Int = 1,
        availableDays: List<DayOfWeek> = emptyList()
    ): ShopItem

    /**
     * 获取该类别中指定 ID 的商品。
     *
     * @param id 要获取的 ID
     * @return 获取到的商品，若不存在则为空
     */
    fun getItem(id: String): ShopItem?

    /**
     * 检查该类别中是否有指定 ID 的商品。
     *
     * @param id 要检查的 ID
     * @return 是否有指定 ID 的商品
     */
    fun hasItem(id: String): Boolean

    /**
     * 移除该类别中指定 ID 的商品。
     *
     * @param id 要移除的 ID
     * @return 被移除的商品，若不存在则为空
     */
    fun removeItem(id: String): ShopItem?
}
