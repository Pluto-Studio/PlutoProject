package ink.pmc.essentials.api.warp

import ink.pmc.framework.inject.inlinedGet
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.World
import java.util.*

@Suppress("UNUSED")
interface WarpManager {
    companion object : WarpManager by inlinedGet()

    val blacklistedWorlds: Collection<World>
    val nameLengthLimit: Int

    suspend fun get(id: UUID): Warp?

    suspend fun get(name: String): Warp?

    suspend fun getSpawn(id: UUID): Warp?

    suspend fun getSpawn(name: String): Warp?

    suspend fun setSpawn(warp: Warp, spawn: Boolean)

    suspend fun getDefaultSpawn(): Warp?

    suspend fun setDefaultSpawn(warp: Warp, default: Boolean)

    suspend fun getPreferredSpawn(player: OfflinePlayer): Warp?

    suspend fun setPreferredSpawn(player: OfflinePlayer, spawn: Warp)

    suspend fun getCollection(player: OfflinePlayer): Collection<Warp>

    suspend fun getCollectionPageCount(player: OfflinePlayer, pageSize: Int): Int

    suspend fun getCollectionByPage(player: OfflinePlayer, pageSize: Int, page: Int): Collection<Warp>

    suspend fun addToCollection(player: OfflinePlayer, warp: Warp)

    suspend fun removeFromCollection(player: OfflinePlayer, warp: Warp)

    suspend fun list(): Collection<Warp>

    suspend fun listSpawns(): Collection<Warp>

    suspend fun listByCategory(category: WarpCategory): Collection<Warp>

    suspend fun getPageCount(pageSize: Int, category: WarpCategory? = null): Int

    suspend fun listByPage(pageSize: Int, page: Int, category: WarpCategory? = null): Collection<Warp>

    suspend fun has(id: UUID): Boolean

    suspend fun has(name: String): Boolean

    suspend fun create(
        name: String,
        location: Location,
        alias: String? = null,
        founder: OfflinePlayer? = null,
        icon: Material? = null,
        category: WarpCategory? = null,
        description: Component? = null,
    ): Warp

    suspend fun remove(id: UUID)

    suspend fun remove(name: String)

    suspend fun update(warp: Warp)

    fun isBlacklisted(world: World): Boolean
}