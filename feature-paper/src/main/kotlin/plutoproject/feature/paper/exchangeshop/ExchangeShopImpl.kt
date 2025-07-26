package plutoproject.feature.paper.exchangeshop

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ExchangeShop
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.feature.paper.exchangeshop.models.UserModel
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import plutoproject.framework.common.util.data.collection.toImmutable
import plutoproject.framework.common.util.data.map.mutableConcurrentMapOf
import plutoproject.framework.paper.util.server
import java.util.*

class ExchangeShopImpl : ExchangeShop, KoinComponent {
    private val userRepo by inject<UserRepository>()
    private val config by inject<ExchangeShopConfig>()
    private val internalCategories = mutableMapOf<String, ShopCategory>()
    private val users = mutableConcurrentMapOf<UUID, ShopUser>()

    override val categories: Collection<ShopCategory> = internalCategories.values.toImmutable()

    override suspend fun getUser(player: Player): ShopUser? {
        return getUser(player.uniqueId)
    }

    override suspend fun getUser(uniqueId: UUID): ShopUser? {
        if (users.containsKey(uniqueId)) {
            return users.getValue(uniqueId)
        }

        val model = userRepo.find(uniqueId) ?: return null
        val user = ShopUserImpl(
            uniqueId = model.uniqueId,
            player = server.getOfflinePlayer(model.uniqueId),
            ticket = model.ticket,
            internalLastTicketRecoveryOn = model.lastTicketRecoveryOn
        )

        users[uniqueId] = user
        return user
    }

    override suspend fun hasUser(player: Player): Boolean {
        return hasUser(player.uniqueId)
    }

    override suspend fun hasUser(uniqueId: UUID): Boolean {
        if (users.containsKey(uniqueId)) return true
        return userRepo.has(uniqueId)
    }

    override suspend fun createUser(player: Player): ShopUser {
        return createUser(player.uniqueId)
    }

    override suspend fun createUser(uniqueId: UUID): ShopUser {
        require(!hasUser(uniqueId)) { "Shop user with ID `$uniqueId` already exists" }

        val user = ShopUserImpl(
            uniqueId = uniqueId,
            player = server.getOfflinePlayer(uniqueId),
            ticket = config.ticket.recoveryCap,
            internalLastTicketRecoveryOn = null,
        )
        val model = UserModel(
            uniqueId = user.uniqueId,
            ticket = user.ticket,
            lastTicketRecoveryOn = user.lastTicketRecoveryOn,
        )

        userRepo.insert(model)
        users[uniqueId] = user
        return user
    }

    override suspend fun getUserOrCreate(player: Player): ShopUser {
        return getUserOrCreate(player.uniqueId)
    }

    override suspend fun getUserOrCreate(uniqueId: UUID): ShopUser {
        return getUser(uniqueId) ?: createUser(uniqueId)
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
        return internalCategories[id]
    }

    override fun hasCategory(id: String): Boolean {
        return internalCategories.containsKey(id)
    }

    override fun removeCategory(id: String): ShopCategory? {
        return internalCategories.remove(id)
    }
}
