package plutoproject.feature.paper.randomTeleport

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import net.kyori.adventure.text.Component
import net.minecraft.server.level.ChunkLevel
import net.minecraft.server.level.FullChunkStatus
import net.minecraft.server.level.TicketType
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.randomTeleport.*
import plutoproject.feature.paper.api.randomTeleport.events.RandomTeleportEvent
import plutoproject.feature.paper.api.teleport.ManagerState
import plutoproject.feature.paper.api.teleport.TaskState
import plutoproject.feature.paper.api.teleport.TeleportManager
import plutoproject.feature.paper.teleport.TELEPORT_FAILED_SOUND
import plutoproject.feature.paper.teleport.TELEPORT_FAILED_TITLE
import plutoproject.feature.paper.teleport.TeleportConfig
import plutoproject.framework.common.util.chat.ECONOMY_SYMBOL
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.chat.toCurrencyFormattedString
import plutoproject.framework.common.util.chat.toFormattedComponent
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.common.util.coroutine.withDefault
import plutoproject.framework.common.util.data.map.mapKeysAndValues
import plutoproject.framework.common.util.trimmedString
import plutoproject.framework.paper.util.hook.vaultHook
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import plutoproject.framework.paper.util.world.chunk.addTicket
import plutoproject.framework.paper.util.world.chunk.removeTicket
import plutoproject.framework.paper.util.world.location.Position2D
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

internal fun Chunk.addTeleportTicket() {
    if (hasTeleportTicket()) {
        return
    }
    addTicket(TicketType.PLUGIN_TICKET, x, z, ChunkLevel.byStatus(FullChunkStatus.FULL), plugin)
}

internal fun Chunk.removeTeleportTicket() {
    removeTicket(TicketType.PLUGIN_TICKET, x, z, ChunkLevel.byStatus(FullChunkStatus.FULL), plugin)
}

internal fun Chunk.hasTeleportTicket(): Boolean {
    return pluginChunkTickets.contains(plugin)
}

class RandomTeleportManagerImpl : RandomTeleportManager, KoinComponent {
    private val config by inject<RandomTeleportConfig>()
    private val teleportConfig by inject<TeleportConfig>()
    private var waitedTicks: Long = -1
    private var inTeleport = ConcurrentHashMap.newKeySet<Player>()
    private var cooldownMap = ConcurrentHashMap<Player, Cooldown>()

    override val cacheTasks: Deque<CacheTask> = LinkedBlockingDeque()
    override val caches: ListMultimap<World, RandomTeleportCache> =
        Multimaps.synchronizedListMultimap(ArrayListMultimap.create())
    override val cooldown: Duration = config.cooldown
    override val defaultOptions: RandomTeleportOptions = RandomTeleportOptions(
        center = Position2D(config.default.center.x, config.default.center.z),
        spawnPointAsCenter = config.default.spawnpointAsCenter,
        chunkPreserveRadius = if (config.default.chunkPreserveRadius >= 0) config.default.chunkPreserveRadius else teleportConfig.default.chunkPrepareRadius,
        cacheAmount = config.default.cacheAmount,
        startRadius = config.default.startRadius,
        endRadius = config.default.endRadius,
        maxHeight = config.default.maxHeight,
        minHeight = config.default.minHeight,
        noCover = config.default.noCover,
        maxAttempts = config.default.maxAttempts,
        cost = config.default.cost,
        blacklistedBiomes = config.default.blacklistedBiomes.toSet()
    )
    override val worldOptions: Map<World, RandomTeleportOptions> = config.worlds.filter { (key, _) ->
        server.worlds.any { it.name == key }
    }.mapKeysAndValues { (key, value) ->
        server.getWorld(key)!! to RandomTeleportOptions(
            center = Position2D(value.center.x, value.center.z),
            spawnPointAsCenter = value.spawnpointAsCenter,
            chunkPreserveRadius = if (value.chunkPreserveRadius >= 0) value.chunkPreserveRadius else defaultOptions.chunkPreserveRadius,
            cacheAmount = value.cacheAmount,
            startRadius = value.startRadius,
            endRadius = value.endRadius,
            maxHeight = value.maxHeight,
            minHeight = value.minHeight,
            noCover = value.noCover,
            maxAttempts = value.maxAttempts,
            cost = value.cost,
            blacklistedBiomes = value.blacklistedBiomes.toSet()
        )
    }
    override val enabledWorlds: Collection<World> = config.enabledWorlds
        .filter { name -> server.worlds.any { it.name == name } }
        .map { server.getWorld(it)!! }
    override var tickCount: Long = 0L
    override var lastTickTime: Long = 0L
    override var state: ManagerState = ManagerState.IDLE

    private val hasUnfinishedTick: Boolean
        get() = state == ManagerState.TICKING

    override fun getRandomTeleportOptions(world: World): RandomTeleportOptions {
        return worldOptions[world] ?: defaultOptions
    }

    override fun getCenterLocation(world: World, options: RandomTeleportOptions?): Position2D {
        val opt = options ?: getRandomTeleportOptions(world)
        val spawnPoint = Position2D(world.spawnLocation)
        val center = opt.center
        val spawnPointAsCenter = opt.spawnPointAsCenter
        return if (spawnPointAsCenter) spawnPoint else center
    }

    override fun getCaches(world: World): Collection<RandomTeleportCache> {
        return caches.get(world)
    }

    override fun pollCache(world: World): RandomTeleportCache? {
        if (getCaches(world).isEmpty()) return null
        val cache = caches.get(world).removeFirst()
        cache.preservedChunks.forEach { it.removeTeleportTicket() }
        return cache
    }

    override suspend fun randomOnce(world: World, options: RandomTeleportOptions?): Location? {
        val opt = options ?: getRandomTeleportOptions(world)
        val rad = opt.endRadius - opt.startRadius
        val range = -rad..rad
        val center = getCenterLocation(world, options)
        val baseX = center.x.toInt()
        val baseZ = center.z.toInt()

        require(opt.startRadius < opt.endRadius) { "startRadius must less than endRadius" }

        fun random(): Int {
            val rand = range.random()
            val offset = if (rand >= 0) opt.startRadius else -opt.startRadius
            return rand + offset
        }

        fun biome(location: Location): Boolean {
            return opt.blacklistedBiomes.contains(location.block.biome)
        }

        fun cover(location: Location): Boolean {
            for (y in 0..(world.maxHeight - location.blockY)) {
                val loc = location.clone().add(0.0, y.toDouble(), 0.0)
                if (!loc.block.type.isAir) return true
            }
            return false
        }

        suspend fun safeLocation(x: Int, z: Int): Location? {
            val maxHeight = if (opt.maxHeight != -1) opt.maxHeight else world.maxHeight
            val minHeight = if (opt.minHeight != -1) opt.minHeight else world.minHeight

            for (y in maxHeight downTo minHeight) {
                val stand = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                val loc = stand.clone().add(0.0, 1.0, 0.0)

                if (!stand.block.type.isSolid) continue
                if (biome(loc)) continue
                if (!TeleportManager.isSafe(loc)) continue
                if (opt.noCover && cover(loc)) continue

                return loc
            }

            return null
        }

        suspend fun searchLocation(): Location? {
            val dx = baseX + random()
            val dz = baseZ + random()
            return safeLocation(dx, dz)
        }

        return withDefault { searchLocation() }
    }

    override suspend fun random(world: World, options: RandomTeleportOptions?): RandomResult {
        val opt = options ?: getRandomTeleportOptions(world)
        return withDefault<RandomResult> {
            var attempts = 0
            repeat(opt.maxAttempts) {
                attempts++
                val loc = randomOnce(world, opt)
                if (loc != null) return@withDefault RandomResult(attempts, loc)
            }
            return@withDefault RandomResult(attempts, null)
        }
    }

    override fun submitCache(world: World, options: RandomTeleportOptions?): CacheTask {
        val opt = options ?: getRandomTeleportOptions(world)
        val task = CacheTaskImpl(world, opt)
        cacheTasks.offer(task)
        return task
    }

    override fun submitCacheFirst(world: World, options: RandomTeleportOptions?): CacheTask {
        val opt = options ?: getRandomTeleportOptions(world)
        val task = CacheTaskImpl(world, opt)
        cacheTasks.offerFirst(task)
        return task
    }

    override fun hasCacheTask(id: UUID): Boolean {
        return cacheTasks.any { it.id == id }
    }

    override fun isInCooldown(player: Player): Boolean {
        return getCooldown(player) != null
    }

    override fun getCooldown(player: Player): Cooldown? {
        return cooldownMap[player]
    }

    override fun launch(
        player: Player,
        world: World,
        options: RandomTeleportOptions?,
        prompt: Boolean
    ) {
        runAsync {
            launchSuspend(player, world, options, prompt)
        }
    }

    private class TeleportTimer {
        private val start: Long = System.currentTimeMillis()
        private var end: Long? = null

        fun end(): Long {
            end = System.currentTimeMillis()
            return end!! - start
        }
    }

    private fun notifyPlayerOfTeleport(
        player: Player,
        location: Location,
        attempts: Int,
        time: Long,
        costed: Boolean,
        cost: Double,
        symbol: String,
        prompt: Boolean
    ) {
        if (!prompt) {
            return
        }
        val message = if (!costed) RANDOM_TELEPORTED else RANDOM_TELEPORTED_WITH_COST
        player.sendMessage(
            message
                .replace(
                    "<location>",
                    Component.text("${location.blockX}, ${location.blockY}, ${location.blockZ}")
                )
                .replace("<amount>", cost.trimmedString())
                .replace("<symbol>", symbol)
                .replace("<attempts>", Component.text(attempts))
                .replace(
                    "<lastLookupTime>",
                    java.time.Duration.ofMillis(time).toKotlinDuration().toFormattedComponent()
                )
        )
    }

    override suspend fun launchSuspend(
        player: Player,
        world: World,
        options: RandomTeleportOptions?,
        prompt: Boolean
    ) {
        withDefault {
            check(!isInCooldown(player)) { "Player ${player.name} is still in cooldown" }
            if (inTeleport.contains(player)) {
                if (prompt) {
                    player.sendMessage(RANDOM_TELEPORT_FAILED_IN_PROGRESS)
                }
                return@withDefault
            }

            val defaultOpt = getRandomTeleportOptions(world)
            val opt = options ?: defaultOpt

            val economy = vaultHook?.economy
            val plural = economy?.currencyNamePlural() ?: ECONOMY_SYMBOL
            val singular = economy?.currencyNameSingular() ?: ECONOMY_SYMBOL
            val cost = opt.cost
            val symbol = if (cost <= 1) singular else plural
            var costed = false

            if (economy != null && cost > 0.0 && !player.hasPermission(RANDOM_TELEPORT_COST_BYPASS_PERMISSION)) {
                val balance = economy.getBalance(player)
                if (balance < cost) {
                    player.sendMessage(
                        RANDOM_TELEPORT_FAILED_BALANCE_NOT_ENOUGH
                            .replace("<amount>", cost.toCurrencyFormattedString())
                            .replace("<symbol>", symbol)
                            .replace("<balance>", balance.toCurrencyFormattedString())
                    )
                    return@withDefault
                }
                economy.withdrawPlayer(player, cost)
                costed = true
            }

            val cache = if (opt == defaultOpt) pollCache(world) else null
            val timer = TeleportTimer()
            inTeleport.add(player)

            val attempts: Int
            val location = if (cache != null) {
                attempts = cache.attempts
                cache.location
            } else {
                if (prompt) {
                    player.showTitle(RANDOM_TELEPORT_SEARCH_TITLE)
                    player.playSound(RANDOM_TELEPORT_SEARCH_SOUND)
                }
                val random = random(world, opt)
                attempts = random.attempts
                random.location
            }

            if (location == null) {
                if (prompt) {
                    player.showTitle(TELEPORT_FAILED_TITLE)
                    player.playSound(TELEPORT_FAILED_SOUND)
                    player.sendMessage(RANDOM_TELEPORT_SEARCH_FAILED)
                }
                inTeleport.remove(player)
                return@withDefault
            }

            // 必须异步触发
            val event = RandomTeleportEvent(player, player.location, location).apply { callEvent() }
            if (event.isCancelled) return@withDefault

            TeleportManager.teleportSuspend(player, location, prompt = prompt)
            val time = timer.end()
            notifyPlayerOfTeleport(player, location, attempts, time, costed, cost, symbol, prompt)
            if (!player.hasPermission(RANDOM_TELEPORT_COOLDOWN_BYPASS_PERMISSION)) {
                cooldownMap[player] = CooldownImpl(cooldown) {
                    cooldownMap.remove(player)
                }
            }
            inTeleport.remove(player)
        }
    }

    override fun isEnabled(world: World): Boolean {
        return enabledWorlds.contains(world)
    }

    private suspend fun tickCacheTask() {
        val task = cacheTasks.poll() ?: return
        val cache = task.tick()
        when (task.state) {
            TaskState.PENDING -> {}
            TaskState.TICKING -> {}
            TaskState.FINISHED -> {
                if (cache != null) {
                    caches.put(cache.world, cache)
                }
            }
        }
    }

    private suspend fun refreshChunkCache() = supervisorScope {
        caches.forEach { _, it ->
            val preserve =
                TeleportManager.getRequiredChunks(it.location, getRandomTeleportOptions(it.world).chunkPreserveRadius)
            val shouldRefresh = preserve.any { chunk ->
                !chunk.isChunkLoaded(it.world) || !chunk.coordinateChunkInBlocking(it.world).hasTeleportTicket()
            }
            if (!shouldRefresh) return@forEach
            launch {
                TeleportManager.prepareChunk(preserve, it.world)
                preserve.forEach { chunk -> chunk.coordinateChunkIn(it.world).addTeleportTicket() }
            }
        }
    }

    private fun tryEmitTasks() {
        enabledWorlds.forEach {
            val amount = getRandomTeleportOptions(it).cacheAmount
            val pending = cacheTasks.filter { t -> t.world == it }.size
            val spare = amount - (getCaches(it).size + pending)

            if (spare <= 0) {
                return@forEach
            }

            repeat(spare) { _ ->
                submitCacheFirst(it, getRandomTeleportOptions(it))
            }
        }
    }

    override suspend fun tick() {
        if (hasUnfinishedTick) {
            return
        }

        tryEmitTasks()

        if (cacheTasks.isEmpty()) {
            return
        }

        if (waitedTicks != -1L && waitedTicks < config.cacheInterval) {
            waitedTicks++
            return
        }

        state = ManagerState.TICKING
        val start = System.currentTimeMillis()

        tickCacheTask()
        refreshChunkCache()

        val end = System.currentTimeMillis()
        lastTickTime = end - start
        tickCount++
        state = ManagerState.IDLE
        waitedTicks = 0
    }
}
