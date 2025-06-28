package plutoproject.feature.paper.warp

import com.sksamuel.aedile.core.cacheBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bson.types.ObjectId
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.warp.Warp
import plutoproject.feature.paper.api.warp.WarpCategory
import plutoproject.feature.paper.api.warp.WarpManager
import plutoproject.feature.paper.api.warp.WarpType
import plutoproject.framework.common.api.playerdb.PlayerDB
import plutoproject.framework.common.util.data.collection.subListOrNull
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.common.util.data.convertToUuidOrNull
import plutoproject.framework.common.util.environment.serverName
import plutoproject.framework.paper.util.data.models.toModel
import plutoproject.framework.paper.util.server
import java.util.*
import java.util.concurrent.ConcurrentMap
import kotlin.math.ceil
import kotlin.time.Duration

class WarpManagerImpl : WarpManager, KoinComponent {
    private val config by inject<WarpConfig>()
    private val repo by inject<WarpRepository>()
    private val preferredSpawnKey = "essentials.$serverName.warp.preferred_spawn"
    private val collectionKey = "essentials.$serverName.warp.collection"
    private val cache = cacheBuilder<UUID, Warp?> { // null 值不会被存储到缓存
        refreshAfterWrite = Duration.parse("5m")
    }.build {
        // 报错会被捕获
        repo.findById(it)?.let { model -> WarpImpl(model) }
    }

    override val blacklistedWorlds: Collection<World> = config.blacklistedWorlds
        .filter { name -> server.worlds.any { it.name == name } }
        .map { server.getWorld(it)!! }
    override val nameLengthLimit: Int = config.nameLengthLimit

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
        val database = PlayerDB.getOrCreate(player.uniqueId)
        val spawnId = database.getString(preferredSpawnKey)?.convertToUuidOrNull() ?: return getDefaultSpawn()
        val spawn = get(spawnId) ?: return getDefaultSpawn()
        return if (spawn.isSpawn) spawn else getDefaultSpawn()
    }

    override suspend fun setPreferredSpawn(player: OfflinePlayer, spawn: Warp) {
        require(spawn.isSpawn) { "Warp ${spawn.name} isn't a spawn" }
        val database = PlayerDB.getOrCreate(player.uniqueId)
        database[preferredSpawnKey] = spawn.id.toString()
        database.update()
    }

    override suspend fun getCollection(player: OfflinePlayer): Collection<Warp> {
        return PlayerDB.getOrCreate(player.uniqueId).getList<String>(collectionKey)
            ?.mapNotNull { get(it.convertToUuid()) }
            ?: emptyList()
    }

    override suspend fun getCollectionPageCount(player: OfflinePlayer, pageSize: Int): Int {
        return ceil(getCollection(player).size.toDouble() / pageSize).toInt()
    }

    override suspend fun getCollectionByPage(player: OfflinePlayer, pageSize: Int, page: Int): Collection<Warp> {
        val list = getCollection(player).sortedByDescending { it.createdAt }
        val skip = page * pageSize
        return list.subListOrNull(skip, skip + pageSize)
    }

    override suspend fun addToCollection(player: OfflinePlayer, warp: Warp) {
        val list = getCollection(player).toMutableList()
        list.add(warp)
        val db = PlayerDB.getOrCreate(player.uniqueId)
        db[collectionKey] = list.map { it.id.toString() }
        db.update()
    }

    override suspend fun removeFromCollection(player: OfflinePlayer, warp: Warp) {
        val list = getCollection(player).toMutableList()
        list.remove(warp)
        val db = PlayerDB.getOrCreate(player.uniqueId)
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
            description?.let { GsonComponentSerializer.gson().serialize(it) },
            WarpType.WARP,
            System.currentTimeMillis(),
            location.toModel(),
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
