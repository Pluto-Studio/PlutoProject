package plutoproject.feature.paper.api.exchangeshop

import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.framework.common.util.inject.Koin

/**
 * 兑换商店主接口。
 */
interface ExchangeShop {
    companion object : ExchangeShop by Koin.get()

    fun declareCategory(
        id: String,
        icon: Material,
        name: Component,
        description: List<Component>
    )
}
