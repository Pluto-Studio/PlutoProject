package plutoproject.feature.paper.exchangeshop

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.exchangeshop.ShopCategory
import plutoproject.feature.paper.api.exchangeshop.ShopItem
import plutoproject.feature.paper.api.exchangeshop.ShopUser
import plutoproject.feature.paper.exchangeshop.models.UserModel
import plutoproject.feature.paper.exchangeshop.repositories.UserRepository
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.common.util.coroutine.createSupervisorChild
import plutoproject.framework.common.util.data.collection.toImmutable
import plutoproject.framework.common.util.data.flow.getValue
import plutoproject.framework.common.util.data.flow.setValue
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
    private val loadedUsers = mutableConcurrentMapOf<UUID, InternalShopUser>()
    private val usersLoadedAt = mutableConcurrentMapOf<ShopUser, Instant>()
    private val autoUnloadJob: Job
    private var isValid by MutableStateFlow(true)

    override val categories: Collection<ShopCategory> = internalCategories.values.toImmutable()
    override val items: Collection<ShopItem>
        get() = categories.flatMap { it.items }
    override val coroutineScope: CoroutineScope = PluginScope.createSupervisorChild()

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
        loadedUsers.forEach { (uniqueId, user) ->
            val loadedAt = usersLoadedAt.getValue(user)
            if (loadedAt.plusSeconds(UNLOAD_AFTER_SECONDS).isBefore(currentTimestamp)
                && server.getPlayer(uniqueId) == null
            ) {
                unloadUserWithoutLock(uniqueId)
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

    override suspend fun getUser(player: OfflinePlayer): ShopUser? {
        check(isValid) { "Instance is not valid" }
        return getUser(player.uniqueId)
    }

    override suspend fun getUser(uniqueId: UUID): ShopUser? = userLock.withLock {
        check(isValid) { "Instance is not valid" }
        if (loadedUsers.containsKey(uniqueId)) {
            return loadedUsers.getValue(uniqueId)
        }

        val model = userRepo.find(uniqueId) ?: return null
        val user = ShopUserImpl(model)

        loadUserWithoutLock(user)
        return user
    }

    override suspend fun hasUser(player: OfflinePlayer): Boolean {
        check(isValid) { "Instance is not valid" }
        return hasUser(player.uniqueId)
    }

    override suspend fun hasUser(uniqueId: UUID): Boolean = userLock.withLock {
        check(isValid) { "Instance is not valid" }
        return hasUserWithoutLock(uniqueId)
    }

    override suspend fun createUser(player: OfflinePlayer): ShopUser {
        check(isValid) { "Instance is not valid" }
        return createUser(player.uniqueId)
    }

    private suspend fun hasUserWithoutLock(uniqueId: UUID): Boolean {
        if (loadedUsers.containsKey(uniqueId)) return true
        return userRepo.has(uniqueId)
    }

    override suspend fun createUser(uniqueId: UUID): ShopUser = userLock.withLock {
        check(isValid) { "Instance is not valid" }
        require(!hasUserWithoutLock(uniqueId)) { "Shop user with ID `$uniqueId` already exists" }

        val model = UserModel(
            uniqueId = uniqueId,
            createTime = Instant.now(),
            ticket = config.ticket.recoveryCap,
            lastTicketRecoveryTime = null,
            scheduledTicketRecoveryTime = null,
        )
        val user = ShopUserImpl(model)

        userRepo.insert(model)
        loadUserWithoutLock(user)
        return user
    }

    override suspend fun getUserOrCreate(player: OfflinePlayer): ShopUser {
        check(isValid) { "Instance is not valid" }
        return getUserOrCreate(player.uniqueId)
    }

    override suspend fun getUserOrCreate(uniqueId: UUID): ShopUser {
        check(isValid) { "Instance is not valid" }
        return getUser(uniqueId) ?: createUser(uniqueId)
    }

    override fun isUserLoaded(id: UUID): Boolean {
        check(isValid) { "Instance is not valid" }
        return loadedUsers.containsKey(id)
    }

    override suspend fun loadUser(user: InternalShopUser) = userLock.withLock {
        check(isValid) { "Instance is not valid" }
        loadUserWithoutLock(user)
    }

    override suspend fun unloadUser(id: UUID) = userLock.withLock {
        check(isValid) { "Instance is not valid" }
        unloadUserWithoutLock(id)
    }

    private fun loadUserWithoutLock(user: InternalShopUser) {
        loadedUsers[user.uniqueId] = user
        usersLoadedAt[user] = Instant.now()
    }

    private suspend fun unloadUserWithoutLock(id: UUID) {
        val user = loadedUsers.remove(id) ?: return
        user.close()
        usersLoadedAt.remove(user)
    }

    override fun createCategory(
        id: String,
        icon: Material,
        name: Component,
        description: List<Component>
    ): ShopCategory {
        check(isValid) { "Instance is not valid" }
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
        check(isValid) { "Instance is not valid" }
        return internalCategories[id]
    }

    override fun hasCategory(id: String): Boolean {
        check(isValid) { "Instance is not valid" }
        return internalCategories.containsKey(id)
    }

    override fun removeCategory(id: String): ShopCategory? {
        check(isValid) { "Instance is not valid" }
        return internalCategories.remove(id)
    }

    override suspend fun shutdown() {
        check(isValid) { "Instance is not valid" }
        loadedUsers.forEach { (_, user) -> user.close() }
        loadedUsers.clear()
        usersLoadedAt.clear()
        autoUnloadJob.cancelAndJoin()
        coroutineScope.coroutineContext[Job]?.cancelAndJoin()
        isValid = false
    }
}
