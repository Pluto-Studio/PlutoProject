package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.AnimatedImageData
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.TileRect
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.core.display.DisplayGeometry
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.render.tile.codec.TileDecoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToLong

@OptIn(ExperimentalUnsignedTypes::class)
class AnimatedDisplayJob(
    override val belongsTo: UUID,
    private val displayScheduler: DisplayScheduler,
    private val viewPort: ViewPort,
    private val displayManager: DisplayManager,
    private val clock: Clock,
    private val maxFramesPerSecond: Int,
    private val visibleDistance: Double,
) : DisplayJob {
    override var isStopped: Boolean = false
        private set

    override val managedDisplayInstances
        get() = _managedDisplayInstances

    private val _managedDisplayInstances = linkedMapOf<UUID, DisplayInstance>()
    private val displayGeometryByInstanceId = HashMap<UUID, DisplayGeometry>()
    private val decodedTilesByTileId = HashMap<Int, ByteArray>()

    private var image: Image? = null
    private var imageDataEntry: ImageDataEntry<*>? = null
    private var animationStartedAtMillis: Long? = null
    private var lastSentPoolIndexes: IntArray? = null

    init {
        require(visibleDistance > 0.0) { "visibleDistance must be greater than 0" }
        require(maxFramesPerSecond == -1 || maxFramesPerSecond > 0) {
            "maxFramesPerSecond must be -1 or greater than 0"
        }
    }

    override fun attach(
        displayInstance: DisplayInstance,
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ) {
        check(!isStopped) { "DisplayJob is stopped" }
        validateSharedObjects(displayInstance, image, imageDataEntry)

        if (this.image == null) {
            this.image = image
            this.imageDataEntry = imageDataEntry
        }

        _managedDisplayInstances[displayInstance.id] = displayInstance
        displayGeometryByInstanceId[displayInstance.id] = displayInstance.buildGeometry()
    }

    override fun detach(displayInstanceId: UUID): DisplayInstance? {
        if (isStopped) {
            return null
        }

        displayGeometryByInstanceId.remove(displayInstanceId)
        return _managedDisplayInstances.remove(displayInstanceId)
    }

    override fun isEmpty(): Boolean = _managedDisplayInstances.isEmpty()

    override fun wake() {
        if (isStopped || _managedDisplayInstances.isEmpty()) {
            return
        }

        val wakeStartedAt = clock.instant()
        val wakeStartedAtMillis = wakeStartedAt.toEpochMilli()
        val image = image ?: return
        val animatedData = imageDataEntry?.asAnimatedData() ?: return
        if (animatedData.frameCount <= 0 || !animatedData.duration.isPositive()) {
            return
        }

        val startedAtMillis = animationStartedAtMillis ?: wakeStartedAtMillis.also {
            animationStartedAtMillis = it
        }
        val elapsedMillis = (wakeStartedAtMillis - startedAtMillis).coerceAtLeast(0L)
        val progressMillis = elapsedMillis % animatedData.duration.inWholeMilliseconds
        val frameIndex = frameIndexAt(progressMillis, animatedData)
        val framePoolIndexes = framePoolIndexes(frameIndex, image.mapWidthBlocks * image.mapHeightBlocks, animatedData)
        val changedTileIds = collectChangedTileIds(framePoolIndexes)

        if (changedTileIds.isNotEmpty()) {
            val visibleTileIdsByPlayer = collectVisibleTileIdsByPlayer(image)
            val decodedTilesByTileId = this.decodedTilesByTileId.also { it.clear() }

            visibleTileIdsByPlayer.forEach { (playerId, visibleTileIds) ->
                val sendJob = displayManager.getLoadedSendJob(playerId) ?: return@forEach

                visibleTileIds.forEach { tileId ->
                    if (tileId !in changedTileIds) {
                        return@forEach
                    }

                    val mapColors = decodedTilesByTileId.getOrPut(tileId) {
                        val tilePoolIndex = framePoolIndexes[tileId]
                        TileDecoder.decode(animatedData.tilePool.getTile(tilePoolIndex).toByteArray())
                    }
                    sendJob.enqueue(MapUpdate(mapId = image.tileMapIds[tileId], mapColors = mapColors))
                }
            }
        }

        lastSentPoolIndexes = framePoolIndexes

        if (!isStopped && _managedDisplayInstances.isNotEmpty()) {
            displayScheduler.scheduleAwakeAt(this, nextAwakeAt(wakeStartedAt))
        }
    }

    override fun stop() {
        if (isStopped) {
            return
        }

        isStopped = true
        _managedDisplayInstances.clear()
        displayGeometryByInstanceId.clear()
        decodedTilesByTileId.clear()
        image = null
        imageDataEntry = null
        animationStartedAtMillis = null
        lastSentPoolIndexes = null
    }

    private fun collectVisibleTileIdsByPlayer(image: Image): Map<UUID, LinkedHashSet<Int>> {
        val visibleTileIdsByPlayer = LinkedHashMap<UUID, LinkedHashSet<Int>>()

        _managedDisplayInstances.values
            .groupBy(DisplayInstance::world)
            .forEach { (world, instances) ->
                val playerViews = viewPort.getPlayerViews(world)
                if (playerViews.isEmpty()) {
                    return@forEach
                }

                instances.forEach { displayInstance ->
                    val geometry = displayGeometryByInstanceId[displayInstance.id]
                        ?: displayInstance.buildGeometry().also {
                            displayGeometryByInstanceId[displayInstance.id] = it
                        }

                    geometry.computeVisibleTiles(
                        playerViews = playerViews,
                        visibleDistance = visibleDistance,
                    ).forEach { (playerView, rect) ->
                        val visibleTileIds = visibleTileIdsByPlayer.computeIfAbsent(playerView.id) {
                            LinkedHashSet()
                        }
                        collectTileIds(rect, image.mapWidthBlocks, visibleTileIds)
                    }
                }
            }

        return visibleTileIdsByPlayer
    }

    private fun frameIndexAt(progressMillis: Long, animatedData: AnimatedImageData): Int {
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
        animatedData: AnimatedImageData,
    ): IntArray {
        val base = frameIndex * singleFrameTileCount
        return IntArray(singleFrameTileCount) { tileId ->
            animatedData.tileIndexes[base + tileId].toInt() and 0xFFFF
        }
    }

    private fun collectChangedTileIds(framePoolIndexes: IntArray): Set<Int> {
        val previous = lastSentPoolIndexes ?: return framePoolIndexes.indices.toSet()
        return buildSet {
            framePoolIndexes.indices.forEach { tileId ->
                if (previous[tileId] != framePoolIndexes[tileId]) {
                    add(tileId)
                }
            }
        }
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

    private fun validateSharedObjects(
        displayInstance: DisplayInstance,
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ) {
        require(displayInstance.belongsTo == belongsTo) {
            "DisplayInstance belongsTo mismatch: expected=$belongsTo, actual=${displayInstance.belongsTo}"
        }
        require(image.id == belongsTo) {
            "Image id mismatch: expected=$belongsTo, actual=${image.id}"
        }
        require(image.type == ImageType.ANIMATED) {
            "AnimatedDisplayJob requires animated image, actual=${image.type}"
        }
        require(imageDataEntry.belongsTo == belongsTo) {
            "ImageDataEntry belongsTo mismatch: expected=$belongsTo, actual=${imageDataEntry.belongsTo}"
        }
        require(imageDataEntry.type == ImageType.ANIMATED) {
            "AnimatedDisplayJob requires animated image data, actual=${imageDataEntry.type}"
        }
    }

    private fun collectTileIds(rect: TileRect, mapWidthBlocks: Int, output: MutableSet<Int>) {
        for (y in rect.minY..rect.maxY) {
            for (x in rect.minX..rect.maxX) {
                output += y * mapWidthBlocks + x
            }
        }
    }
}
