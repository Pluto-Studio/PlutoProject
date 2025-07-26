package plutoproject.feature.paper.exchangeshop

import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.feature.paper.exchangeshop.models.UserModel
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.common.util.data.collection.toImmutable
import plutoproject.framework.common.util.data.map.mutableConcurrentMapOf
import plutoproject.framework.paper.util.server
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.seconds

class ExchangeShopImpl : InternalExchangeShop, KoinComponent {
    private val userRepo by inject<UserRepository>()
    private val config by inject<ExchangeShopConfig>()
    private val internalCategories = mutableMapOf<String, ShopCategory>()
    private val users = mutableConcurrentMapOf<UUID, ShopUser>()
    private val userLastUsedTimestamps = mutableConcurrentMapOf<ShopUser, Instant>()
    private val autoUnloadJob = runAutoUnloadDaemonJob()

    override val categories: Collection<ShopCategory> = internalCategories.values.toImmutable()

    private fun runAutoUnloadDaemonJob(): Job = runAsync {
        while (isActive) {
            delay(AUTO_UNLOAD_INTERVAL_SECONDS.seconds)
            unloadUnusedUsers()
        }
    }

    private fun unloadUnusedUsers() {
        val currentTimestamp = Instant.now()
        val iterator = users.iterator()
        while (iterator.hasNext()) {
            val (id, user) = iterator.next()
            val lastUsed = getLastUsedTimestamp(user)
            if (lastUsed.plusSeconds(MAX_UNUSED_SECONDS).isBefore(currentTimestamp)
                && server.getPlayer(id) == null
            ) {
                iterator.remove()
                userLastUsedTimestamps.remove(user)
            }
        }
    }

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

        loadUser(user)
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
        loadUser(user)
        return user
    }

    override suspend fun getUserOrCreate(player: Player): ShopUser {
        return getUserOrCreate(player.uniqueId)
    }

    override suspend fun getUserOrCreate(uniqueId: UUID): ShopUser {
        return getUser(uniqueId) ?: createUser(uniqueId)
    }

    override fun isUserLoaded(id: UUID): Boolean {
        return users.containsKey(id)
    }

    override fun loadUser(user: ShopUser) {
        users[user.uniqueId] = user
        userLastUsedTimestamps[user] = Instant.now()
    }

    override fun unloadUser(id: UUID) {
        userLastUsedTimestamps.remove(users.remove(id) ?: return)
    }

    override fun getLastUsedTimestamp(user: ShopUser): Instant {
        return userLastUsedTimestamps.getValue(user)
    }

    override fun setUsed(user: ShopUser) {
        if (!userLastUsedTimestamps.containsKey(user)) return
        userLastUsedTimestamps[user] = Instant.now()
    }

    override fun createCategory(
        id: String,
        icon: Material,
        name: Component,
        description: List<Component>
    ): ShopCategory {
        require(id.isAlphabeticOrUnderscore()) { "ID must contain only English letters and underscores: $id" }
        require(!hasCategory(id)) { "Shop category with ID `$id` already exists" }

        val category = ShopCategoryImpl(
            id = id,
            icon = icon,
            name = name,
            description = description,
        )

        internalCategories[id] = category
        return category
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

    override fun shutdown() = runBlocking {
        autoUnloadJob.cancelAndJoin()
    }
}
