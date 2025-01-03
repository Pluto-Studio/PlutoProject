package ink.pmc.essentials.home

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import ink.pmc.essentials.api.home.Home
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.essentials.config.EssentialsConfig
import ink.pmc.essentials.disabled
import ink.pmc.essentials.essentialsScope
import ink.pmc.essentials.models.HomeModel
import ink.pmc.essentials.repositories.HomeRepository
import ink.pmc.framework.platform.paper
import ink.pmc.framework.storage.model
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.*
import kotlin.time.Duration.Companion.minutes

internal fun loadFailed(id: UUID, reason: String): String {
    return "Failed to load Home $id: $reason"
}

class HomeManagerImpl : HomeManager, KoinComponent {
    private val config by lazy { get<EssentialsConfig>().home }
    private val repo by inject<HomeRepository>()

    override val maxHomes: Int = config.maxHomes
    override val nameLengthLimit: Int = config.nameLengthLimit
    override val blacklistedWorlds: Collection<World> = config.blacklistedWorlds
        .filter { name -> paper.worlds.any { it.name == name } }
        .map { paper.getWorld(it)!! }
    override val loadedHomes: ListMultimap<OfflinePlayer, Home> =
        Multimaps.synchronizedListMultimap<OfflinePlayer, Home>(ArrayListMultimap.create())

    init {
        essentialsScope.launch {
            while (!disabled) {
                delay(5.minutes)
                loadedHomes.entries().removeIf { !it.key.isOnline }
            }
        }
    }

    override fun isLoaded(id: UUID): Boolean {
        return getLoaded(id) != null
    }

    override fun isLoaded(player: OfflinePlayer, name: String): Boolean {
        return getLoaded(player, name) != null
    }

    override fun unload(id: UUID) {
        loadedHomes.values().removeIf { it.id == id }
    }

    override fun unload(player: OfflinePlayer, name: String) {
        loadedHomes.get(player).removeIf { it.name == name }
    }

    override fun unloadAll(player: OfflinePlayer) {
        loadedHomes.removeAll(player)
    }

    private fun getLoaded(id: UUID): Home? {
        return loadedHomes.values().firstOrNull { it.id == id }
    }

    private fun getLoaded(player: OfflinePlayer, name: String): Home? {
        return loadedHomes.get(player).firstOrNull { it.name == name }
    }

    override suspend fun get(id: UUID): Home? {
        val loaded = getLoaded(id) ?: run {
            val model = repo.findById(id) ?: return null
            val home = HomeImpl(model)
            loadedHomes.put(home.owner, home)
            home
        }
        return loaded
    }

    override suspend fun get(player: OfflinePlayer, name: String): Home? {
        val loaded = getLoaded(player, name) ?: run {
            val model = repo.findByName(player, name) ?: return null
            val home = HomeImpl(model)
            loadedHomes.put(home.owner, home)
            home
        }
        return loaded
    }

    override suspend fun getPreferredHome(player: OfflinePlayer): Home? {
        return list(player).firstOrNull { it.isPreferred }
    }

    override suspend fun setPreferredHome(home: Home) {
        home.setPreferred(true)
    }

    override suspend fun unsetPreferredHome(home: Home) {
        home.setPreferred(false)
    }

    override suspend fun list(player: OfflinePlayer): Collection<Home> {
        val model = repo.findByPlayer(player)
        val homes = model.mapNotNull { get(it.id) }
        return homes
    }

    override suspend fun has(id: UUID): Boolean {
        if (isLoaded(id)) return true
        return repo.hasById(id)
    }

    override suspend fun has(player: OfflinePlayer, name: String): Boolean {
        if (isLoaded(player, name)) return true
        return repo.hasByName(player, name)
    }

    override suspend fun hasHome(player: OfflinePlayer): Boolean {
        return list(player).isNotEmpty()
    }

    override suspend fun create(owner: OfflinePlayer, name: String, location: Location): Home {
        require(!has(owner, name)) { "Home of player ${owner.name} named $name already existed" }
        require(name.length <= nameLengthLimit) { "Name $name doesn't match the requirement" }
        val model = HomeModel(
            ObjectId(),
            UUID.randomUUID(),
            name,
            null,
            System.currentTimeMillis(),
            location.model,
            owner.uniqueId,
        )
        val home = HomeImpl(model)
        loadedHomes.put(owner, home)
        repo.save(model)
        return home
    }

    override suspend fun remove(player: OfflinePlayer, name: String) {
        if (isLoaded(player, name)) unload(player, name)
        repo.deleteByName(player, name)
    }

    override suspend fun remove(id: UUID) {
        if (isLoaded(id)) unload(id)
        repo.deleteById(id)
    }

    override suspend fun update(home: Home) {
        home.update()
    }

    override fun isBlacklisted(world: World): Boolean {
        return blacklistedWorlds.contains(world)
    }
}