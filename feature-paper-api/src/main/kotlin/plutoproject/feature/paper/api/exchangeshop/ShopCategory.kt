package plutoproject.feature.paper.api.exchangeshop

import net.kyori.adventure.text.Component
import org.bukkit.Material

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
     * 该类别包含的物品。
     */
    val items: Collection<ShopItem>


    /**
     * 获取该类别中指定 ID 的物品。
     *
     * @param id 需要获取的 ID
     * @return 获取到的物品，若不存在则为空
     */
    fun getItem(id: String): ShopItem?
}
