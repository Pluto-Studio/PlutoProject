package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.display.DisplayGeometry
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.PlayerView
import plutoproject.feature.gallery.core.display.TileRect
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.util.VisibleTileSet
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.HashMap
import java.util.HashSet
import java.util.UUID
import kotlin.math.roundToLong

class AnimatedDisplayJob(
    override val imageId: UUID,
    private val image: Image,
    initialResource: AnimatedDisplayResource,
    private val displayScheduler: DisplayScheduler,
    private val viewPort: ViewPort,
    private val sendJobRegistry: SendJobRegistry,
    private val clock: Clock,
    private val maxFramesPerSecond: Int,
    private val visibleDistance: Double,
) : DisplayJob {
    override val type: ImageType = ImageType.ANIMATED

    override val isStopped: Boolean
        get() = synchronized(lock) { stopped }

    override val attachedDisplayInstances: Map<UUID, DisplayInstance>
        get() = synchronized(lock) { attachedInstances.toMap() }

    private val lock = Any()
    private val attachedInstances = HashMap<UUID, DisplayInstance>()
    private val displayGeometryByInstanceId = HashMap<UUID, DisplayGeometry>()
    private val visibleTileIdsByPlayer = HashMap<UUID, VisibleTileSet>()
    private val activePlayerIds = HashSet<UUID>()
    private val playerViewsByWorld = HashMap<String, List<PlayerView>>()
    private val maxTileCount = image.widthBlocks * image.heightBlocks

    private var stopped = false
    private var resource: AnimatedDisplayResource = initialResource
    private var animationStartedAtMillis: Long? = null
    private var lastSentPoolIndexes: IntArray? = null

    init {
        require(image.id == imageId) {
            "Image id mismatch: expected=$imageId, actual=${image.id}"
        }
        require(image.type == ImageType.ANIMATED) {
            "AnimatedDisplayJob requires animated image, actual=${image.type}"
        }
        require(visibleDistance > 0.0) { "visibleDistance must be greater than 0" }
        require(maxFramesPerSecond == -1 || maxFramesPerSecond > 0) {
            "maxFramesPerSecond must be -1 or greater than 0"
        }
    }

    override fun attach(displayInstance: DisplayInstance) {
        synchronized(lock) {
            check(!stopped) { "DisplayJob is stopped" }
            require(displayInstance.imageId == imageId) {
                "DisplayInstance imageId mismatch: expected=$imageId, actual=${displayInstance.imageId}"
            }

            attachedInstances[displayInstance.id] = displayInstance
            displayGeometryByInstanceId[displayInstance.id] = displayInstance.buildGeometry()
            displayScheduler.scheduleAwakeAt(this, clock.instant())
        }
    }

    override fun detach(displayInstanceId: UUID): DisplayInstance? {
        return synchronized(lock) {
            if (stopped) {
                return@synchronized null
            }

            displayGeometryByInstanceId.remove(displayInstanceId)
            attachedInstances.remove(displayInstanceId)
        }
    }

    override fun replaceResource(resource: DisplayResource) {
        require(resource is AnimatedDisplayResource) {
            "AnimatedDisplayJob requires animated display resource, actual=${resource::class.simpleName}"
        }

        synchronized(lock) {
            check(!stopped) { "DisplayJob is stopped" }
            this.resource = resource
            animationStartedAtMillis = null
            lastSentPoolIndexes = null
            if (attachedInstances.isNotEmpty()) {
                displayScheduler.scheduleAwakeAt(this, clock.instant())
            }
        }
    }

    override fun isEmpty(): Boolean {
        return synchronized(lock) {
            attachedInstances.isEmpty()
        }
    }

    override fun wake() {
        synchronized(lock) {
            if (stopped || attachedInstances.isEmpty()) {
                return
            }

            val wakeStartedAt = clock.instant()
            val framePoolIndexes = currentFramePoolIndexes(wakeStartedAt.toEpochMilli(), resource)
            if (hasNoChangedTiles(framePoolIndexes)) {
                lastSentPoolIndexes = framePoolIndexes
                scheduleNextAwake(wakeStartedAt)
                return
            }

            playerViewsByWorld.clear()
            activePlayerIds.clear()
            visibleTileIdsByPlayer.values.forEach(VisibleTileSet::clear)

            collectVisibleTileIds()
            pruneInactivePlayerTileCaches()
            sendVisibleTiles(resource, framePoolIndexes)

            lastSentPoolIndexes = framePoolIndexes
            scheduleNextAwake(wakeStartedAt)
        }
    }

    override fun stop() {
        synchronized(lock) {
            if (stopped) {
                return
            }

            stopped = true
            displayScheduler.unschedule(this)
            attachedInstances.clear()
            displayGeometryByInstanceId.clear()
            visibleTileIdsByPlayer.clear()
            activePlayerIds.clear()
            playerViewsByWorld.clear()
            animationStartedAtMillis = null
            lastSentPoolIndexes = null
        }
    }

    fun removePlayerCache(player: UUID) {
        synchronized(lock) {
            visibleTileIdsByPlayer.remove(player)
        }
    }

    private fun currentFramePoolIndexes(
        wakeStartedAtMillis: Long,
        resource: AnimatedDisplayResource,
    ): IntArray {
        val startedAtMillis = animationStartedAtMillis ?: wakeStartedAtMillis.also {
            animationStartedAtMillis = it
        }
        val elapsedMillis = (wakeStartedAtMillis - startedAtMillis).coerceAtLeast(0L)
        val progressMillis = elapsedMillis % resource.duration.inWholeMilliseconds
        val frameIndex = frameIndexAt(progressMillis, resource)
        return framePoolIndexes(frameIndex, maxTileCount, resource)
    }

    private fun hasNoChangedTiles(framePoolIndexes: IntArray): Boolean {
        val previous = lastSentPoolIndexes ?: return false
        framePoolIndexes.indices.forEach { tileId ->
            if (previous[tileId] != framePoolIndexes[tileId]) {
                return false
            }
        }
        return true
    }

    private fun scheduleNextAwake(wakeStartedAt: Instant) {
        if (stopped || attachedInstances.isEmpty()) {
            return
        }
        displayScheduler.scheduleAwakeAt(this, nextAwakeAt(wakeStartedAt))
    }

    private fun nextAwakeAt(wakeStartedAt: Instant): Instant {
        if (maxFramesPerSecond == -1) {
            return clock.instant()
        }

        val wakeFinishedAt = clock.instant()
        val elapsedMillis = Duration.between(wakeStartedAt, wakeFinishedAt).toMillis().coerceAtLeast(0)
        val budgetMillis = 1000.0 / maxFramesPerSecond.toDouble()
        val waitMillis = (budgetMillis - elapsedMillis.toDouble()).coerceAtLeast(0.0)
        return wakeFinishedAt.plusMillis(waitMillis.roundToLong())
    }

    private fun frameIndexAt(progressMillis: Long, resource: AnimatedDisplayResource): Int {
        if (resource.frameCount == 1) {
            return 0
        }

        val frameDuration = resource.duration.inWholeMilliseconds.toDouble() / resource.frameCount.toDouble()
        return (progressMillis / frameDuration)
            .toInt()
            .coerceIn(0, resource.frameCount - 1)
    }

    private fun framePoolIndexes(
        frameIndex: Int,
        singleFrameTileCount: Int,
        resource: AnimatedDisplayResource,
    ): IntArray {
        val base = frameIndex * singleFrameTileCount
        return IntArray(singleFrameTileCount) { tileId ->
            resource.tileIndexes[base + tileId].toInt() and 0xFFFF
        }
    }

    private fun collectVisibleTileIds() {
        attachedInstances.forEach { (_, instance) ->
            val playerViews = playerViewsByWorld.getOrPut(instance.world) {
                viewPort.getPlayerViews(instance.world).also { views ->
                    views.forEach { activePlayerIds.add(it.id) }
                }
            }

            if (playerViews.isEmpty()) {
                return@forEach
            }

            collectTileIdsForInstance(playerViews, instance)
        }
    }

    private fun collectTileIdsForInstance(playerViews: List<PlayerView>, instance: DisplayInstance) {
        val geometry = displayGeometryByInstanceId.getOrPut(instance.id) {
            instance.buildGeometry()
        }

        playerViews.forEach { playerView ->
            val rect = geometry.computeVisibleTiles(playerView, visibleDistance) ?: return@forEach
            val visibleTileIds = visibleTileIdsByPlayer.getOrPut(playerView.id) {
                VisibleTileSet(maxTileCount)
            }
            collectTileIds(rect, image.widthBlocks, visibleTileIds)
        }
    }

    private fun collectTileIds(rect: TileRect, mapWidthBlocks: Int, output: VisibleTileSet) {
        for (y in rect.minY..rect.maxY) {
            for (x in rect.minX..rect.maxX) {
                output.add(y * mapWidthBlocks + x)
            }
        }
    }

    private fun pruneInactivePlayerTileCaches() {
        visibleTileIdsByPlayer.keys.removeIf { playerId -> playerId !in activePlayerIds }
    }

    private fun sendVisibleTiles(
        resource: AnimatedDisplayResource,
        framePoolIndexes: IntArray,
    ) {
        visibleTileIdsByPlayer.forEach { (playerId, visibleTileIds) ->
            if (visibleTileIds.size == 0) {
                return@forEach
            }
            sendPlayerVisibleTiles(playerId, visibleTileIds, resource, framePoolIndexes)
        }
    }

    private fun sendPlayerVisibleTiles(
        playerId: UUID,
        visibleTileIds: VisibleTileSet,
        resource: AnimatedDisplayResource,
        framePoolIndexes: IntArray,
    ) {
        val sendJob = sendJobRegistry.get(playerId) ?: return
        val previousPoolIndexes = lastSentPoolIndexes

        visibleTileIds.forEach { tileId ->
            if (previousPoolIndexes != null && previousPoolIndexes[tileId] == framePoolIndexes[tileId]) {
                return@forEach
            }

            val tilePoolIndex = framePoolIndexes[tileId]
            sendJob.enqueue(MapUpdate(image.tileMapIds[tileId], resource.decodedTilePool.getTile(tilePoolIndex)))
        }
    }
}
