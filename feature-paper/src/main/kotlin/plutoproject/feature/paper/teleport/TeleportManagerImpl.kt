package plutoproject.feature.paper.teleport

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.api.teleport.*
import plutoproject.feature.paper.api.teleport.events.TeleportEvent
import plutoproject.framework.common.util.chat.MESSAGE_SOUND
import plutoproject.framework.common.util.chat.UNUSUAL_ISSUE_OCCURRED
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.chat.title.replaceSubTitle
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.common.util.coroutine.raceConditional
import plutoproject.framework.common.util.data.map.mapKeysAndValues
import plutoproject.framework.common.util.time.toFormattedComponent
import plutoproject.framework.paper.util.coroutine.coroutineContext
import plutoproject.framework.paper.util.entity.teleportSuspend
import plutoproject.framework.paper.util.server
import plutoproject.framework.paper.util.world.chunk.ChunkLocation
import plutoproject.framework.paper.util.world.getChunkFromSource
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds

class TeleportManagerImpl : TeleportManager, KoinComponent {
    private val config by inject<TeleportConfig>()

    // 用于在完成队列任务时通知
    private val notifyChannel = Channel<UUID>()

    override val teleportRequests: MutableList<TeleportRequest> = mutableListOf()
    override val queue: Queue<TeleportTask> = ConcurrentLinkedQueue()
    override val defaultRequestOptions: RequestOptions = RequestOptions(
        config.request.expireAfter,
        config.request.removeAfter
    )
    override val defaultTeleportOptions: TeleportOptions = TeleportOptions(
        false,
        config.default.avoidVoid,
        config.default.safeLocationSearchRadius,
        config.default.chunkPrepareRadius,
        config.default.blacklistedBlocks.toSet(),
    )
    override val worldTeleportOptions: Map<World, TeleportOptions> = config.worlds.filter { (key, _) ->
        server.worlds.any { it.name == key }
    }.mapKeysAndValues { (key, value) ->
        server.getWorld(key)!! to TeleportOptions(
            false,
            value.avoidVoid,
            value.safeLocationSearchRadius,
            value.chunkPrepareRadius,
            value.blacklistedBlocks.toSet()
        )
    }
    override val blacklistedWorlds: Collection<World> = config.blacklistedWorlds
        .filter { name -> server.worlds.any { it.name == name } }
        .map { server.getWorld(it)!! }
    override val locationCheckers: MutableMap<String, LocationChecker> = mutableMapOf()
    override var tickingTask: TeleportTask? = null
    override var tickCount = 0L
    override var lastTickTime = 0L
    override var state: TeleportManagerState = TeleportManagerState.IDLE

    init {
        registerLocationChecker("void") { l, o ->
            if (o.avoidVoid) l.y >= l.world.minHeight else true
        }
        registerLocationChecker("collision") { l, _ ->
            val foot = !l.block.isCollidable
            val head = !l.clone().add(0.0, 1.0, 0.0).block.isCollidable
            val stand = l.clone().subtract(0.0, 1.0, 0.0).block.isCollidable
            foot && head && stand
        }
        registerLocationChecker("blacklist") { l, o ->
            val foot = !o.blacklistedBlocks.contains(l.block.type)
            val head = !o.blacklistedBlocks.contains(l.clone().add(0.0, 1.0, 0.0).block.type)
            val stand = !o.blacklistedBlocks.contains(l.clone().subtract(0.0, 1.0, 0.0).block.type)
            foot && head && stand
        }
        registerLocationChecker("world_border") { l, _ ->
            l.world.worldBorder.isInside(l)
        }
    }

    override fun getWorldTeleportOptions(world: World): TeleportOptions {
        return world.teleportOptions
    }

    private val hasUnfinishedTick: Boolean
        get() = state == TeleportManagerState.TICKING

    private val World.teleportOptions: TeleportOptions
        get() = worldTeleportOptions[this] ?: defaultTeleportOptions

    private fun Location.chunkNeedToPrepare(radius: Int): List<ChunkLocation> {
        val centerChunk = ChunkLocation(chunk.x, chunk.z)
        val chunks = (-radius..radius).flatMap { x ->
            (-radius..radius).map { z ->
                val x1 = centerChunk.x + x
                val y1 = centerChunk.z + z
                ChunkLocation(x1, y1)
            }
        }.toMutableList()
        chunks.add(centerChunk)
        return chunks
    }

    private fun List<ChunkLocation>.allPrepared(world: World): Boolean {
        return all { it.isChunkLoaded(world) }
    }

    private suspend fun prepareSingleChunk(world: World, loc: ChunkLocation) {
        when (config.chunkPrepareMethod) {
            ChunkPrepareMethod.SYNC -> world.getChunkAt(loc.x, loc.z)
            ChunkPrepareMethod.ASYNC -> world.getChunkAtAsync(loc.x, loc.z).await()
            ChunkPrepareMethod.ASYNC_FAST -> world.getChunkAtAsyncUrgently(loc.x, loc.z).await()
            ChunkPrepareMethod.CHUNK_SOURCE -> world.getChunkFromSource(loc.x, loc.z)
        }
    }

    @JvmName("internalPrepareChunk")
    private suspend fun Collection<ChunkLocation>.prepareChunk(world: World) = supervisorScope {
        forEach {
            prepareSingleChunk(world, it)
            yield() // 确保任务超时可以正常取消
        }
    }

    private suspend fun Location.searchSafeLoc(options: TeleportOptions): Location? {
        val visited = ConcurrentHashMap.newKeySet<Location>()

        suspend fun Location.bfs(): Location? {
            val radius = options.safeLocationSearchRadius
            val queue: Queue<Location> = LinkedList()
            queue.add(this)

            while (queue.isNotEmpty()) {
                val current = queue.poll()
                if (!visited.add(current)) {
                    continue
                }

                if (isSafe(current, options)) {
                    return current
                }

                // 添加相邻位置
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        for (dz in -1..1) {
                            if (dx == 0 && dy == 0 && dz == 0) continue // 跳过当前位置
                            val neighbor = current.clone().add(dx.toDouble(), dy.toDouble(), dz.toDouble())
                            if (neighbor.distance(this) <= radius) {
                                queue.add(neighbor)
                            }
                        }
                    }
                }
            }

            return null
        }

        suspend fun iterateLocations(range: IntProgression, direction: BlockFace): Location? {
            for (dy in range) {
                val loc = when (direction) {
                    BlockFace.UP -> clone().add(0.0, dy.toDouble(), 0.0)
                    BlockFace.DOWN -> clone().subtract(0.0, dy.toDouble(), 0.0)
                    else -> throw IllegalStateException("Unsupported direction")
                }

                if (direction == BlockFace.UP && loc.blockY > world.maxHeight) {
                    return null
                }

                if (direction == BlockFace.DOWN && loc.blockY < world.minHeight) {
                    return null
                }

                val bfs = loc.bfs()

                if (bfs != null) {
                    return bfs
                }

                yield() // 尽快响应取消
            }

            return null
        }

        return raceConditional(
            PluginScope.async { iterateLocations(0..(world.maxHeight - blockY), BlockFace.UP) },
            PluginScope.async { bfs() },
            PluginScope.async { iterateLocations(0..(blockY - world.minHeight), BlockFace.DOWN) }
        ) { it != null }?.toCenterLocation()?.apply { y = floor(y) }
    }

    override suspend fun fireTeleport(
        player: Player,
        destination: Location,
        options: TeleportOptions?,
        prompt: Boolean
    ) = withContext(Dispatchers.Default) {
        val opt = options ?: destination.world.teleportOptions
        val loc = if (opt.disableSafeCheck || isSafe(destination, opt)) {
            destination
        } else {
            destination.searchSafeLoc(opt)
        }
        val title = if (loc == destination) teleportSucceedTitle else teleportSucceedTitleSafe

        if (loc == null) {
            if (prompt) {
                player.showTitle(TELEPORT_FAILED_TITLE)
                player.playSound(TELEPORT_FAILED_SOUND)
            }
            return@withContext
        }

        // 必须异步触发
        val event = TeleportEvent(player, player.location, loc, opt).apply { callEvent() }

        if (event.isDenied) {
            if (prompt) {
                val reason = event.deniedReason ?: TELEPORT_DENIED_REASON_DEFAULT
                player.showTitle(TELEPORT_FAILED_DENIED_TITLE.replaceSubTitle("<reason>", reason))
                player.playSound(TELEPORT_FAILED_SOUND)
            }
            return@withContext
        }

        if (event.isCancelled) {
            return@withContext
        }

        // 1.21.11 更新后传送玩家似乎不会自动关闭打开的容器了？手动关闭一下
        withContext(server.coroutineContext) {
            player.closeInventory()
        }
        player.teleportSuspend(loc)

        if (prompt) {
            player.showTitle(title)
            player.playSound(TELEPORT_SUCCEED_SOUND)
        }
    }

    override fun getRequest(id: UUID): TeleportRequest? {
        return teleportRequests.firstOrNull { it.id == id }
    }

    override fun getSentRequests(player: Player): Collection<TeleportRequest> {
        return teleportRequests.filter { it.source == player }
    }

    override fun getReceivedRequests(player: Player): Collection<TeleportRequest> {
        return teleportRequests.filter { it.destination == player }
    }

    override fun hasRequest(id: UUID): Boolean {
        return teleportRequests.any { it.id == id }
    }

    override fun hasUnfinishedRequest(player: Player): Boolean {
        return getSentRequests(player).any { !it.isFinished }
    }

    override fun getUnfinishedRequest(player: Player): TeleportRequest? {
        return getSentRequests(player).firstOrNull { !it.isFinished }
    }

    override fun hasPendingRequest(player: Player): Boolean {
        return getReceivedRequests(player).any { !it.isFinished }
    }

    override fun getPendingRequest(player: Player): TeleportRequest? {
        return getReceivedRequests(player).firstOrNull { !it.isFinished }
    }

    override fun createRequest(
        source: Player,
        destination: Player,
        direction: TeleportDirection,
        options: RequestOptions
    ): TeleportRequest? {
        require(source != destination) { "Source cannot equals to destination" }
        if (hasUnfinishedRequest(source) || hasPendingRequest(destination)) {
            return null
        }

        if (config.blacklistedWorlds.contains(destination.world.name)) {
            return null
        }

        val request = TeleportRequestImpl(options, source, destination, direction)
        val message = when (direction) {
            TeleportDirection.GO -> TELEPORT_TPA_RECEIVED.replace("<player>", source.name)
            TeleportDirection.COME -> TELEPORT_TPAHERE_RECEIVED.replace("<player>", source.name)
        }

        destination.sendMessage(message)
        destination.sendMessage(TELEPORT_EXPIRE.replace("<expire>", options.expireAfter.toFormattedComponent()))
        destination.sendMessage(getTeleportOperationMessage(request))
        destination.playSound(MESSAGE_SOUND)

        if (teleportRequests.size == config.maxRequestsStored) {
            teleportRequests.removeAt(0).cancel()
        }

        teleportRequests.add(request)

        PluginScope.launch {
            delay(options.expireAfter)
            if (!hasRequest(request.id) || request.isFinished) {
                return@launch
            }
            request.expire()
            destination.sendMessage(TELEPORT_REQUEST_EXPIRED.replace("<player>", source.name))
            destination.playSound(TELEPORT_REQUEST_CANCELLED_SOUND)
        }

        PluginScope.launch {
            delay(options.removeAfter)
            if (!hasRequest(request.id)) {
                return@launch
            }
            removeRequest(request.id)
        }

        return request
    }

    override fun cancelRequest(id: UUID) {
        getRequest(id)?.cancel()
    }

    override fun cancelRequest(request: TeleportRequest) {
        request.cancel()
    }

    override fun removeRequest(id: UUID) {
        if (!hasRequest(id)) {
            return
        }
        getRequest(id)!!.cancel()
        teleportRequests.removeIf { it.id == id }
    }

    override fun clearRequest() {
        teleportRequests.forEach { it.cancel() }
        teleportRequests.clear()
    }

    override fun getRequiredChunks(center: Location, radius: Int): Collection<ChunkLocation> {
        return center.chunkNeedToPrepare(radius)
    }

    override fun isAllPrepared(chunks: Collection<ChunkLocation>, world: World): Boolean {
        return chunks.toList().allPrepared(world)
    }

    override suspend fun prepareChunk(chunks: Collection<ChunkLocation>, world: World) {
        chunks.prepareChunk(world)
    }

    override fun teleport(player: Player, destination: Location, options: TeleportOptions?, prompt: Boolean) {
        PluginScope.launch {
            teleportSuspend(player, destination, options, prompt)
        }
    }

    override fun teleport(player: Player, destination: Player, options: TeleportOptions?, prompt: Boolean) {
        PluginScope.launch {
            teleportSuspend(player, destination.location, options, prompt)
        }
    }

    override fun teleport(player: Player, destination: Entity, options: TeleportOptions?, prompt: Boolean) {
        PluginScope.launch {
            teleportSuspend(player, destination.location, options, prompt)
        }
    }

    override fun registerLocationChecker(id: String, checker: LocationChecker) {
        locationCheckers[id] = checker
    }

    override fun unregisterLocationChecker(id: String) {
        locationCheckers.remove(id)
    }

    override suspend fun isSafe(location: Location, options: TeleportOptions?): Boolean {
        val opt = options ?: location.world.teleportOptions
        return locationCheckers.values.all { it(location, opt) }
    }

    override suspend fun searchSafeLocationSuspend(start: Location, options: TeleportOptions?): Location? {
        val opt = options ?: start.world.teleportOptions
        return start.searchSafeLoc(opt)
    }

    override fun searchSafeLocation(start: Location, options: TeleportOptions?): Location? =
        PluginScope.async { searchSafeLocationSuspend(start, options) }.asCompletableFuture().join()

    override suspend fun teleportSuspend(
        player: Player,
        destination: Location,
        options: TeleportOptions?,
        prompt: Boolean
    ) = withContext(Dispatchers.Default) {
        val optRadius = destination.world.teleportOptions.chunkPrepareRadius
        val vt = player.sendViewDistance
        val radius = if (vt < optRadius) vt else optRadius
        val prepare = destination.chunkNeedToPrepare(radius)

        if (prepare.allPrepared(destination.world)) {
            fireTeleport(player, destination, options, prompt)
            return@withContext
        }

        if (prompt) {
            player.showTitle(TELEPORT_PREPARING_TITLE)
            player.playSound(TELEPORT_PREPARING_SOUND)
        }

        val id = UUID.randomUUID()
        val task = TeleportTaskImpl(id, player, destination, options, prompt, prepare)
        queue.offer(task)

        PluginScope.launch {
            delay(10.seconds)
            if (task.isFinished) {
                return@launch
            }
            task.cancel()
            if (prompt) {
                player.showTitle(TELEPORT_FAILED_TIMEOUT_TITLE)
                player.sendMessage(UNUSUAL_ISSUE_OCCURRED)
                player.playSound(TELEPORT_FAILED_SOUND)
            }
        }

        supervisorScope {
            for (uuid in notifyChannel) {
                if (uuid == id) {
                    break
                }
            }
        }
    }

    override suspend fun teleportSuspend(
        player: Player,
        destination: Player,
        options: TeleportOptions?,
        prompt: Boolean
    ) {
        teleportSuspend(player, destination.location, options, prompt)
    }

    override suspend fun teleportSuspend(
        player: Player,
        destination: Entity,
        options: TeleportOptions?,
        prompt: Boolean
    ) {
        teleportSuspend(player, destination.location, options, prompt)
    }

    override fun isBlacklisted(world: World): Boolean {
        return blacklistedWorlds.contains(world)
    }

    override suspend fun tick() {
        if (hasUnfinishedTick) {
            return
        }

        if (queue.isEmpty()) {
            return
        }

        state = TeleportManagerState.TICKING
        val start = System.currentTimeMillis()

        repeat(config.queueProcessPerTick) {
            val task = queue.poll() ?: return@repeat
            tickingTask = task
            task.tick()
            notifyChannel.send(task.id)
            tickingTask = null
        }

        val end = System.currentTimeMillis()
        lastTickTime = end - start
        tickCount++
        state = TeleportManagerState.IDLE
    }
}
