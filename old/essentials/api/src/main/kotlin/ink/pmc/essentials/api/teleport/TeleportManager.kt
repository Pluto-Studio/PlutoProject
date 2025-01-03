package ink.pmc.essentials.api.teleport

import ink.pmc.framework.inject.inlinedGet
import ink.pmc.framework.world.ValueVec2
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*

enum class TeleportManagerState {
    IDLE, TICKING
}

typealias LocationChecker = suspend (Location, TeleportOptions) -> Boolean

@Suppress("UNUSED")
interface TeleportManager {
    companion object : TeleportManager by inlinedGet()

    val teleportRequests: Collection<TeleportRequest>
    val queue: Queue<TeleportTask>
    val defaultRequestOptions: RequestOptions
    val defaultTeleportOptions: TeleportOptions
    val worldTeleportOptions: Map<World, TeleportOptions>
    val blacklistedWorlds: Collection<World>
    val locationCheckers: Map<String, LocationChecker>
    val tickingTask: TeleportTask?
    val tickCount: Long
    val lastTickTime: Long
    val state: TeleportManagerState

    fun getWorldTeleportOptions(world: World): TeleportOptions

    fun getRequest(id: UUID): TeleportRequest?

    fun hasRequest(id: UUID): Boolean

    fun getSentRequests(player: Player): Collection<TeleportRequest>

    fun getReceivedRequests(player: Player): Collection<TeleportRequest>

    fun hasUnfinishedRequest(player: Player): Boolean

    fun getUnfinishedRequest(player: Player): TeleportRequest?

    fun hasPendingRequest(player: Player): Boolean

    fun getPendingRequest(player: Player): TeleportRequest?

    fun createRequest(
        source: Player,
        destination: Player,
        direction: TeleportDirection,
        options: RequestOptions = defaultRequestOptions,
    ): TeleportRequest?

    fun cancelRequest(id: UUID)

    fun cancelRequest(request: TeleportRequest)

    fun removeRequest(id: UUID)

    fun clearRequest()

    fun getRequiredChunks(center: Location, radius: Int): Collection<ValueVec2>

    fun isAllPrepared(chunks: Collection<ValueVec2>, world: World): Boolean

    suspend fun prepareChunk(chunks: Collection<ValueVec2>, world: World)

    suspend fun fireTeleport(
        player: Player,
        destination: Location,
        options: TeleportOptions?,
        prompt: Boolean = true
    )

    fun teleport(player: Player, destination: Location, options: TeleportOptions? = null, prompt: Boolean = true)

    fun registerLocationChecker(id: String, checker: LocationChecker)

    fun unregisterLocationChecker(id: String)

    suspend fun isSafe(location: Location, options: TeleportOptions? = null): Boolean

    suspend fun searchSafeLocationSuspend(start: Location, options: TeleportOptions? = null): Location?

    fun searchSafeLocation(start: Location, options: TeleportOptions? = null): Location?

    suspend fun teleportSuspend(
        player: Player,
        destination: Location,
        options: TeleportOptions? = null,
        prompt: Boolean = true
    )

    fun teleport(player: Player, destination: Player, options: TeleportOptions? = null, prompt: Boolean = true)

    suspend fun teleportSuspend(
        player: Player,
        destination: Player,
        options: TeleportOptions? = null,
        prompt: Boolean = true
    )

    fun teleport(player: Player, destination: Entity, options: TeleportOptions? = null, prompt: Boolean = true)

    suspend fun teleportSuspend(
        player: Player,
        destination: Entity,
        options: TeleportOptions? = null,
        prompt: Boolean = true
    )

    fun isBlacklisted(world: World): Boolean

    suspend fun tick()
}