package plutoproject.feature.paper.exchangeshop

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopItem
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.feature.paper.exchangeshop.models.UserModel
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import plutoproject.framework.common.util.coroutine.PlutoCoroutineScope
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
    private val userLock = Mutex()
    private val users = mutableConcurrentMapOf<UUID, InternalShopUser>()
    private val userLastUsedTimestamps = mutableConcurrentMapOf<ShopUser, Instant>()
    private val autoUnloadJob: Job

    override val categories: Collection<ShopCategory> = internalCategories.values.toImmutable()
    override val items: Collection<ShopItem>
        get() = categories.flatMap { it.items }
    override val coroutineScope: CoroutineScope = CoroutineScope(
        PlutoCoroutineScope.coroutineContext + Job(PlutoCoroutineScope.coroutineContext[Job])
    )

    init {
        loadConfigDeclaration()
        autoUnloadJob = runAutoUnloadDaemonJob()
    }

    private fun runAutoUnloadDaemonJob(): Job = coroutineScope.launch {
        while (isActive) {
            delay(AUTO_UNLOAD_INTERVAL_SECONDS.seconds)
            unloadUnusedUsers()
        }
    }

    private suspend fun unloadUnusedUsers() = userLock.withLock {
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

    private fun loadConfigDeclaration() {
        config.categories.forEach { addConfigCategory(it) }
        config.items.forEach { addConfigItem(it) }
        val categoryWord = if (categories.size <= 1) "category" else "categories"
        val itemWord = if (items.size <= 1) "item" else "items"
        featureLogger.info("Added ${categories.size} $categoryWord from config")
        featureLogger.info("Added ${items.size} $itemWord from config")
    }

    private fun addConfigCategory(config: ShopCategoryConfig) {
        if (!config.id.isValidIdentifier()) {
            featureLogger.severe("Invalid category ID in config: '${config.id}'")
            return
        }
        createCategory(
            id = config.id,
            icon = config.icon,
            name = config.name,
            description = config.description
        )
    }

    private fun addConfigItem(config: ShopItemConfig) {
        if (!config.id.isValidIdentifier()) {
            featureLogger.severe("Invalid shop item ID in config: '${config.id}'")
            return
        }
        val category = getCategory(config.category)
        if (category == null) {
            featureLogger.severe("Invalid category '${config.category}' in config for item ID: ${config.id}")
            return
        }
        category.addItem(
            id = config.id,
            itemStack = ItemStack(config.material),
            ticketConsumption = config.ticketConsumption,
            price = config.price,
            quantity = config.quantity,
            availableDays = config.availableDays
        )
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
            lastTicketRecoveryOn = model.lastTicketRecoveryOn,
            createdAt = model.createdAt
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

        val model = UserModel(
            uniqueId = uniqueId,
            ticket = config.ticket.recoveryCap,
            lastTicketRecoveryOn = null,
            createdAt = Instant.now()
        )
        val user = ShopUserImpl(model)

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

    override suspend fun loadUser(user: InternalShopUser) = userLock.withLock {
        users[user.uniqueId] = user
        userLastUsedTimestamps[user] = Instant.now()
    }

    override suspend fun unloadUser(id: UUID) = userLock.withLock {
        val user = users.remove(id) ?: return@withLock
        userLastUsedTimestamps.remove(user)
        user.close()
    }

    override fun getLastUsedTimestamp(user: ShopUser): Instant {
        return userLastUsedTimestamps.getValue(user)
    }

    override fun setUsed(user: ShopUser) {
        userLastUsedTimestamps.replace(user, Instant.now())
    }

    override fun createCategory(
        id: String,
        icon: Material,
        name: Component,
        description: List<Component>
    ): ShopCategory {
        require(id.isValidIdentifier()) { "ID must contain only English letters, numbers and underscores: $id" }
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

    override suspend fun shutdown() {
        autoUnloadJob.cancelAndJoin()
        coroutineScope.coroutineContext[Job]?.cancelAndJoin()
    }
}
