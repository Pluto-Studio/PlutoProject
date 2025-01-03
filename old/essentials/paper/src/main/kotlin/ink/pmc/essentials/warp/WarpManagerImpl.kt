package ink.pmc.essentials.warp

import com.sksamuel.aedile.core.cacheBuilder
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpCategory
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.essentials.api.warp.WarpType
import ink.pmc.essentials.config.EssentialsConfig
import ink.pmc.essentials.models.WarpModel
import ink.pmc.essentials.repositories.WarpRepository
import ink.pmc.framework.player.db.PlayerDb
import ink.pmc.framework.chat.gsonComponentSerializer
import ink.pmc.framework.datastructure.safeSubList
import ink.pmc.framework.platform.paper
import ink.pmc.framework.player.uuid
import ink.pmc.framework.player.uuidOrNull
import ink.pmc.framework.storage.model
import net.kyori.adventure.text.Component
import org.bson.types.ObjectId
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.ConcurrentMap
import kotlin.math.ceil
import kotlin.time.Duration

class WarpManagerImpl : WarpManager, KoinComponent {
    private val config by inject<EssentialsConfig>()
    private val warpConfig by lazy { config.warp }
    private val repo by inject<WarpRepository>()
    private val preferredSpawnKey = "essentials.${config.serverName}.warp.preferred_spawn"
    private val collectionKey = "essentials.${config.serverName}.warp.collection"
    private val cache = cacheBuilder<UUID, Warp?> { // null 值不会被存储到缓存
        refreshAfterWrite = Duration.parse("5m")
    }.build {
        // 报错会被捕获
        repo.findById(it)?.let { model -> WarpImpl(model) }
    }

    override val blacklistedWorlds: Collection<World> = warpConfig.blacklistedWorlds
        .filter { name -> paper.worlds.any { it.name == name } }
        .map { paper.getWorld(it)!! }
    override val nameLengthLimit: Int = warpConfig.nameLengthLimit

    private suspend fun isCached(name: String): Boolean {
        return cache.asMap().values.any { it?.name == name }
    }

    private suspend fun invalidate(name: String) {
        (cache.asMap() as ConcurrentMap).entries.removeIf { it.value?.name == name }
    }

    override suspend fun get(id: UUID): Warp? {
        return cache.get(id) ?: repo.findById(id)?.let {
            WarpImpl(it).also { warp -> cache.put(id, warp) }
        }
    }

    override suspend fun get(name: String): Warp? {
        return cache.asMap().values.firstOrNull { it?.name == name }
            ?: repo.findByName(name)?.let {
                WarpImpl(it).also { warp -> cache.put(warp.id, warp) }
            }
    }

    override suspend fun getSpawn(id: UUID): Warp? {
        return listSpawns().firstOrNull { it.id == id }
    }

    override suspend fun getSpawn(name: String): Warp? {
        return listSpawns().firstOrNull { it.name == name }
    }

    override suspend fun setSpawn(warp: Warp, spawn: Boolean) {
        warp.type = when {
            spawn && !warp.isSpawn -> WarpType.SPAWN
            !spawn && warp.isSpawn -> WarpType.WARP
            else -> return
        }
        warp.update()
    }

    override suspend fun getDefaultSpawn(): Warp? {
        return listSpawns().firstOrNull { it.isDefaultSpawn }
    }

    override suspend fun setDefaultSpawn(warp: Warp, default: Boolean) {
        getDefaultSpawn()?.also {
            if (it == warp) return@also
            setDefaultSpawn(it, false)
        }
        warp.type = when {
            default && !warp.isDefaultSpawn -> WarpType.SPAWN_DEFAULT
            !default && warp.isDefaultSpawn -> WarpType.SPAWN
            else -> return
        }
        warp.update()
    }

    override suspend fun getPreferredSpawn(player: OfflinePlayer): Warp? {
        val database = PlayerDb.getOrCreate(player.uniqueId)
        val spawnId = database.getString(preferredSpawnKey)?.uuidOrNull ?: return getDefaultSpawn()
        val spawn = get(spawnId) ?: return getDefaultSpawn()
        return if (spawn.isSpawn) spawn else getDefaultSpawn()
    }

    override suspend fun setPreferredSpawn(player: OfflinePlayer, spawn: Warp) {
        require(spawn.isSpawn) { "Warp ${spawn.name} isn't a spawn" }
        val database = PlayerDb.getOrCreate(player.uniqueId)
        database[preferredSpawnKey] = spawn.id.toString()
        database.update()
    }

    override suspend fun getCollection(player: OfflinePlayer): Collection<Warp> {
        return PlayerDb.getOrCreate(player.uniqueId).getList<String>(collectionKey)
            ?.mapNotNull { get(it.uuid) }
            ?: emptyList()
    }

    override suspend fun getCollectionPageCount(player: OfflinePlayer, pageSize: Int): Int {
        return ceil(getCollection(player).size.toDouble() / pageSize).toInt()
    }

    override suspend fun getCollectionByPage(player: OfflinePlayer, pageSize: Int, page: Int): Collection<Warp> {
        val list = getCollection(player).sortedByDescending { it.createdAt }
        val skip = page * pageSize
        return list.safeSubList(skip, skip + pageSize)
    }

    override suspend fun addToCollection(player: OfflinePlayer, warp: Warp) {
        val list = getCollection(player).toMutableList()
        list.add(warp)
        val db = PlayerDb.getOrCreate(player.uniqueId)
        db[collectionKey] = list.map { it.id.toString() }
        db.update()
    }

    override suspend fun removeFromCollection(player: OfflinePlayer, warp: Warp) {
        val list = getCollection(player).toMutableList()
        list.remove(warp)
        val db = PlayerDb.getOrCreate(player.uniqueId)
        db[collectionKey] = list.map { it.id.toString() }
        db.update()
    }

    override suspend fun list(): Collection<Warp> {
        return repo.find().map {
            WarpImpl(it).also { warp -> cache.put(warp.id, warp) }
        }
    }

    override suspend fun listSpawns(): List<Warp> {
        return repo.findSpawns().map {
            WarpImpl(it).also { warp -> cache.put(warp.id, warp) }
        }
    }

    override suspend fun listByCategory(category: WarpCategory): Collection<Warp> {
        return repo.findByCategory(category).map {
            WarpImpl(it).also { warp -> cache.put(warp.id, warp) }
        }
    }

    override suspend fun getPageCount(pageSize: Int, category: WarpCategory?): Int {
        return repo.getPageCount(pageSize, category)
    }

    override suspend fun listByPage(pageSize: Int, page: Int, category: WarpCategory?): Collection<Warp> {
        return repo.findByPage(pageSize, page, category).map {
            WarpImpl(it).also { warp -> cache.put(warp.id, warp) }
        }
    }

    override suspend fun has(id: UUID): Boolean {
        if (cache.contains(id)) return true
        return repo.hasById(id)
    }

    override suspend fun has(name: String): Boolean {
        if (isCached(name)) return true
        return repo.hasByName(name)
    }

    override suspend fun create(
        name: String,
        location: Location,
        alias: String?,
        founder: OfflinePlayer?,
        icon: Material?,
        category: WarpCategory?,
        description: Component?,
    ): Warp {
        require(!has(name)) { "Warp named $name already existed" }
        val model = WarpModel(
            ObjectId(),
            UUID.randomUUID(),
            name,
            alias,
            founder?.uniqueId?.toString(),
            icon,
            category,
            description?.let { gsonComponentSerializer.serialize(it) },
            WarpType.WARP,
            System.currentTimeMillis(),
            location.model,
        )
        val warp = WarpImpl(model)
        cache.put(model.id, warp)
        repo.save(model)
        return warp
    }

    override suspend fun remove(id: UUID) {
        cache.invalidate(id)
        repo.deleteById(id)
    }

    override suspend fun remove(name: String) {
        invalidate(name)
        repo.deleteByName(name)
    }

    override suspend fun update(warp: Warp) {
        warp.update()
    }

    override fun isBlacklisted(world: World): Boolean {
        return blacklistedWorlds.contains(world)
    }
}