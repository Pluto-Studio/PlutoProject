package plutoproject.feature.paper.api.exchangeshop

import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.framework.common.util.inject.Koin

/**
 * 兑换商店主接口。
 */
interface ExchangeShop {
    companion object : ExchangeShop by Koin.get()

    /**
     * 创建一个新的类别。
     *
     * @param id 类别的 ID，需唯一
     * @param icon 类别在菜单中显示的图标
     * @param name 类别在菜单中显示的名称
     * @param description 类别在菜单中显示的介绍
     * @return 创建的 [ShopCategory]
     * @throws IllegalArgumentException 同 ID 的类别已存在或 ID 不合法
     */
    fun createCategory(
        id: String,
        icon: Material,
        name: Component,
        description: List<Component>
    ): ShopCategory

    /**
     * 移除一个已有的类别，若不存在该 ID 的类别则什么也不发生。
     *
     * @param id 需要移除的类别 ID
     */
    fun removeCategory(id: String)
}
