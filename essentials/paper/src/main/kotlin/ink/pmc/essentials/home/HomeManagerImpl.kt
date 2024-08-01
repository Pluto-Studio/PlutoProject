package ink.pmc.essentials.home

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import ink.pmc.essentials.api.home.Home
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.essentials.config.EssentialsConfig
import ink.pmc.essentials.disabled
import ink.pmc.essentials.dtos.HomeDto
import ink.pmc.essentials.essentialsScope
import ink.pmc.essentials.repositories.HomeRepository
import ink.pmc.utils.concurrent.submitAsync
import ink.pmc.utils.storage.entity.dto
import kotlinx.coroutines.delay
import org.bson.types.ObjectId
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.*
import kotlin.time.Duration.Companion.minutes

internal fun loadFailed(id: UUID, reason: String): String {
    return "Failed to loadAll Home $id: $reason"
}

class HomeManagerImpl : HomeManager, KoinComponent {

    private val conf by lazy { get<EssentialsConfig>().Home() }
    private val repo by inject<HomeRepository>()

    override val maxHomes: Int = conf.maxHomes
    override val nameLengthLimit: Int = conf.nameLengthLimit
    override val blacklistedWorlds: Collection<World> = conf.blacklistedWorlds
    override val loadedHomes: ListMultimap<OfflinePlayer, Home> =
        Multimaps.synchronizedListMultimap<OfflinePlayer, Home>(ArrayListMultimap.create())

    init {
        essentialsScope.submitAsync {
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
            val dto = repo.findById(id) ?: return null
            val home = HomeImpl(dto)
            loadedHomes.put(home.owner, home)
            home
        }
        return loaded
    }

    override suspend fun get(player: OfflinePlayer, name: String): Home? {
        val loaded = getLoaded(player, name) ?: run {
            val dto = repo.findByName(player, name) ?: return null
            val home = HomeImpl(dto)
            loadedHomes.put(home.owner, home)
            home
        }
        return loaded
    }

    override suspend fun list(player: OfflinePlayer): Collection<Home> {
        val dto = repo.findByPlayer(player)
        println("dto: $dto")
        val homes = dto.mapNotNull { get(it.id) }
        println("homes: $homes")
        return homes
    }

    override suspend fun has(player: OfflinePlayer, name: String): Boolean {
        if (getLoaded(player, name) != null) return true
        return repo.hasByName(player, name)
    }

    override suspend fun remove(id: UUID) {
        if (isLoaded(id)) unload(id)
        repo.deleteById(id)
    }

    override suspend fun remove(player: OfflinePlayer, name: String) {
        if (isLoaded(player, name)) unload(player, name)
        repo.deleteByName(player, name)
    }

    override suspend fun create(owner: Player, name: String, location: Location): Home {
        require(!has(owner, name)) { "Home of player ${owner.name} named $name already existed" }
        val dto = HomeDto(
            ObjectId(),
            UUID.randomUUID(),
            name,
            System.currentTimeMillis(),
            location.dto,
            owner.uniqueId
        )
        val home = HomeImpl(dto)
        loadedHomes.put(owner, home)
        repo.save(dto)
        return home
    }

    override suspend fun update(home: Home) {
        home.update()
    }

    override fun isBlacklisted(world: World): Boolean {
        return blacklistedWorlds.contains(world)
    }

}