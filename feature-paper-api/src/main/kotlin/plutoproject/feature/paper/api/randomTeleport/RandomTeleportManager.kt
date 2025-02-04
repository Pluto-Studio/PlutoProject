package plutoproject.feature.paper.api.randomTeleport

import com.google.common.collect.Multimap
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.teleport.ManagerState
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.paper.util.world.location.Position2D
import java.util.*
import kotlin.time.Duration

@Suppress("UNUSED")
interface RandomTeleportManager {
    companion object : RandomTeleportManager by Koin.get()

    val cacheTasks: Queue<CacheTask>
    val caches: Multimap<World, RandomTeleportCache>
    val cooldown: Duration
    val defaultOptions: RandomTeleportOptions
    val worldOptions: Map<World, RandomTeleportOptions>
    val enabledWorlds: Collection<World>
    val tickCount: Long
    val lastTickTime: Long
    val state: ManagerState

    fun getRandomTeleportOptions(world: World): RandomTeleportOptions

    fun getCenterLocation(world: World, options: RandomTeleportOptions? = null): Position2D

    fun getCaches(world: World): Collection<RandomTeleportCache>

    fun pollCache(world: World): RandomTeleportCache?

    suspend fun randomOnce(world: World, options: RandomTeleportOptions? = null): Location?

    suspend fun random(world: World, options: RandomTeleportOptions? = null): RandomResult

    fun submitCache(world: World, options: RandomTeleportOptions? = null): CacheTask

    fun submitCacheFirst(world: World, options: RandomTeleportOptions? = null): CacheTask

    fun hasCacheTask(id: UUID): Boolean

    fun isInCooldown(player: Player): Boolean

    fun getCooldown(player: Player): Cooldown?

    fun launch(player: Player, world: World, options: RandomTeleportOptions? = null, prompt: Boolean = true)

    suspend fun launchSuspend(
        player: Player,
        world: World,
        options: RandomTeleportOptions? = null,
        prompt: Boolean = true
    )

    fun isEnabled(world: World): Boolean

    suspend fun tick()
}
