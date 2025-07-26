package plutoproject.feature.paper.exchangeshop

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import java.util.*

class ExchangeShopImpl : ExchangeShop {
    override suspend fun getUser(player: Player): ShopUser? {
        TODO("Not yet implemented")
    }

    override suspend fun getUser(uniqueId: UUID): ShopUser? {
        TODO("Not yet implemented")
    }

    override suspend fun hasUser(player: Player): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun hasUser(uniqueId: UUID): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createUser(player: Player): ShopUser {
        TODO("Not yet implemented")
    }

    override suspend fun createUser(uniqueId: UUID): ShopUser {
        TODO("Not yet implemented")
    }

    override suspend fun getUserOrCreate(player: Player): ShopUser {
        TODO("Not yet implemented")
    }

    override suspend fun getUserOrCreate(uniqueId: UUID): ShopUser {
        TODO("Not yet implemented")
    }

    override fun createCategory(
        id: String,
        icon: Material,
        name: Component,
        description: List<Component>
    ): ShopCategory {
        TODO("Not yet implemented")
    }

    override fun getCategory(id: String): ShopCategory? {
        TODO("Not yet implemented")
    }

    override fun removeCategory(id: String) {
        TODO("Not yet implemented")
    }
}
