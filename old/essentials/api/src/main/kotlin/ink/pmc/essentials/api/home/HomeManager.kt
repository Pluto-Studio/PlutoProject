package ink.pmc.essentials.api.home

import com.google.common.collect.ListMultimap
import ink.pmc.framework.inject.inlinedGet
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import java.util.*

@Suppress("UNUSED")
interface HomeManager {
    companion object : HomeManager by inlinedGet()

    val maxHomes: Int
    val nameLengthLimit: Int
    val blacklistedWorlds: Collection<World>
    val loadedHomes: ListMultimap<OfflinePlayer, Home>

    fun isLoaded(id: UUID): Boolean

    fun isLoaded(player: OfflinePlayer, name: String): Boolean

    fun unload(id: UUID)

    fun unload(player: OfflinePlayer, name: String)

    fun unloadAll(player: OfflinePlayer)

    suspend fun get(id: UUID): Home?

    suspend fun get(player: OfflinePlayer, name: String): Home?

    suspend fun getPreferredHome(player: OfflinePlayer): Home?

    suspend fun setPreferredHome(home: Home)

    suspend fun unsetPreferredHome(home: Home)

    suspend fun list(player: OfflinePlayer): Collection<Home>

    suspend fun has(id: UUID): Boolean

    suspend fun has(player: OfflinePlayer, name: String): Boolean

    suspend fun hasHome(player: OfflinePlayer): Boolean

    suspend fun create(owner: OfflinePlayer, name: String, location: Location): Home

    suspend fun remove(id: UUID)

    suspend fun remove(player: OfflinePlayer, name: String)

    suspend fun update(home: Home)

    fun isBlacklisted(world: World): Boolean
}