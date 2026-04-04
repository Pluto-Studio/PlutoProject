package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.display.*
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.render.tile.codec.TileDecoder
import plutoproject.feature.gallery.core.util.VisibleTileSet
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.roundToLong

class AnimatedDisplayJob(
    override val belongsTo: UUID,
    private val image: Image,
    private val imageDataEntry: ImageDataEntry.Animated,
    private val displayScheduler: DisplayScheduler,
    private val viewPort: ViewPort,
    private val displayManager: DisplayManager,
    private val clock: Clock,
    private val maxFramesPerSecond: Int,
    private val visibleDistance: Double,
) : DisplayJob {
    override var isStopped: Boolean = false
        private set

    override val attachedDisplayInstances
        get() = _attachedDisplayInstances

    private val _attachedDisplayInstances = HashMap<UUID, DisplayInstance>()
    private val displayGeometryByInstanceId = HashMap<UUID, DisplayGeometry>()
    private val maxTileCount = image.widthBlocks * image.heightBlocks

    // wake 内使用，每轮可能清理
    private val visibleTileIdsByPlayer = HashMap<UUID, VisibleTileSet>()
    private val activePlayerIds = HashSet<UUID>()
    private val decodedTilesByPoolIndex = HashMap<Int, ByteArray>()
    private val playerViewsByWorld = HashMap<String, List<PlayerView>>()

    private var animationStartedAtMillis: Long? = null
    private var lastSentPoolIndexes: IntArray? = null
    private var cachedImageData: ImageData.Animated = imageDataEntry.data

    init {
        validateSharedObjects(image, imageDataEntry)
        require(visibleDistance > 0.0) { "visibleDistance must be greater than 0" }
        require(maxFramesPerSecond == -1 || maxFramesPerSecond > 0) {
            "maxFramesPerSecond must be -1 or greater than 0"
        }
    }

    private fun validateSharedObjects(
        image: Image,
        imageDataEntry: ImageDataEntry.Animated,
    ) {
        require(image.id == belongsTo) {
            "Image id mismatch: expected=$belongsTo, actual=${image.id}"
        }
        require(image.type == ImageType.ANIMATED) {
            "AnimatedDisplayJob requires animated image, actual=${image.type}"
        }
        require(imageDataEntry.imageId == belongsTo) {
            "ImageDataEntry belongsTo mismatch: expected=$belongsTo, actual=${imageDataEntry.imageId}"
        }
        require(imageDataEntry.type == ImageType.ANIMATED) {
            "AnimatedDisplayJob requires animated image data, actual=${imageDataEntry.type}"
        }
    }

    override fun attach(displayInstance: DisplayInstance) {
        check(!isStopped) { "DisplayJob is stopped" }
        validateDisplayInstance(displayInstance)

        _attachedDisplayInstances[displayInstance.id] = displayInstance
        displayGeometryByInstanceId[displayInstance.id] = displayInstance.buildGeometry()
    }

    private fun validateDisplayInstance(displayInstance: DisplayInstance) {
        require(displayInstance.belongsTo == belongsTo) {
            "DisplayInstance belongsTo mismatch: expected=$belongsTo, actual=${displayInstance.belongsTo}"
        }
    }

    override fun detach(displayInstanceId: UUID): DisplayInstance? {
        if (isStopped) {
            return null
        }

        displayGeometryByInstanceId.remove(displayInstanceId)
        return _attachedDisplayInstances.remove(displayInstanceId)
    }

    override fun isEmpty(): Boolean {
        return _attachedDisplayInstances.isEmpty()
    }

    override fun wake() {
        if (isStopped || _attachedDisplayInstances.isEmpty()) {
            return
        }

        val wakeStartedAt = clock.instant()
        val animatedData = currentImageData(wakeStartedAt.toEpochMilli())
        if (animatedData.frameCount <= 0 || !animatedData.duration.isPositive()) {
            return
        }

        val framePoolIndexes = currentFramePoolIndexes(wakeStartedAt.toEpochMilli(), animatedData)
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
        sendVisibleTiles(animatedData, framePoolIndexes)

        lastSentPoolIndexes = framePoolIndexes
        scheduleNextAwake(wakeStartedAt)
    }

    private fun currentImageData(wakeStartedAtMillis: Long): ImageData.Animated {
        val currentData = imageDataEntry.data
        if (cachedImageData !== currentData) {
            cachedImageData = currentData
            decodedTilesByPoolIndex.clear()
            animationStartedAtMillis = wakeStartedAtMillis
            lastSentPoolIndexes = null
        }
        return currentData
    }

    private fun currentFramePoolIndexes(
        wakeStartedAtMillis: Long,
        animatedData: ImageData.Animated,
    ): IntArray {
        val startedAtMillis = animationStartedAtMillis ?: wakeStartedAtMillis.also {
            animationStartedAtMillis = it
        }
        val elapsedMillis = (wakeStartedAtMillis - startedAtMillis).coerceAtLeast(0L)
        val progressMillis = elapsedMillis % animatedData.duration.inWholeMilliseconds
        val frameIndex = frameIndexAt(progressMillis, animatedData)
        return framePoolIndexes(frameIndex, maxTileCount, animatedData)
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
        if (isStopped || _attachedDisplayInstances.isEmpty()) {
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

    private fun frameIndexAt(progressMillis: Long, animatedData: ImageData.Animated): Int {
        if (animatedData.frameCount == 1) {
            return 0
        }

        val frameDuration = animatedData.duration.inWholeMilliseconds.toDouble() / animatedData.frameCount.toDouble()
        return (progressMillis / frameDuration)
            .toInt()
            .coerceIn(0, animatedData.frameCount - 1)
    }

    private fun framePoolIndexes(
        frameIndex: Int,
        singleFrameTileCount: Int,
        animatedData: ImageData.Animated,
    ): IntArray {
        val base = frameIndex * singleFrameTileCount
        return IntArray(singleFrameTileCount) { tileId ->
            animatedData.tileIndexes[base + tileId].toInt() and 0xFFFF
        }
    }

    private fun collectVisibleTileIds() {
        _attachedDisplayInstances.forEach { (_, instance) ->
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
        animatedData: ImageData.Animated,
        framePoolIndexes: IntArray,
    ) {
        visibleTileIdsByPlayer.forEach { (playerId, visibleTileIds) ->
            if (visibleTileIds.size == 0) {
                return@forEach
            }
            sendPlayerVisibleTiles(playerId, visibleTileIds, animatedData, framePoolIndexes)
        }
    }

    private fun sendPlayerVisibleTiles(
        playerId: UUID,
        visibleTileIds: VisibleTileSet,
        animatedData: ImageData.Animated,
        framePoolIndexes: IntArray,
    ) {
        val sendJob = displayManager.getLoadedSendJob(playerId) ?: return
        val previousPoolIndexes = lastSentPoolIndexes

        visibleTileIds.forEach { tileId ->
            if (previousPoolIndexes != null && previousPoolIndexes[tileId] == framePoolIndexes[tileId]) {
                return@forEach
            }

            val tilePoolIndex = framePoolIndexes[tileId]
            val mapColors = decodedTilesByPoolIndex.getOrPut(tilePoolIndex) {
                TileDecoder.decode(animatedData.tilePool.getTile(tilePoolIndex).toByteArray())
            }
            sendJob.enqueue(MapUpdate(image.tileMapIds[tileId], mapColors))
        }
    }

    fun removePlayerCache(player: UUID) {
        visibleTileIdsByPlayer.remove(player)
    }

    override fun stop() {
        if (isStopped) {
            return
        }

        isStopped = true

        _attachedDisplayInstances.clear()
        displayGeometryByInstanceId.clear()

        visibleTileIdsByPlayer.clear()
        activePlayerIds.clear()
        decodedTilesByPoolIndex.clear()
        playerViewsByWorld.clear()

        animationStartedAtMillis = null
        lastSentPoolIndexes = null
    }
}
