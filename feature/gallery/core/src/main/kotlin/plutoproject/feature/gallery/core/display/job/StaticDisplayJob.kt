package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.display.*
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.render.tile.codec.TileDecoder
import java.time.Clock
import java.util.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class StaticDisplayJob(
    override val belongsTo: UUID,
    private val image: Image,
    private val imageDataEntry: ImageDataEntry.Static,
    private val displayScheduler: DisplayScheduler,
    private val viewPort: ViewPort,
    private val displayManager: DisplayManager,
    private val clock: Clock,
    private val visibleDistance: Double,
    private val updateInterval: Duration,
) : DisplayJob {
    override var isStopped: Boolean = false
        private set

    override val managedDisplayInstances
        get() = _managedDisplayInstances

    private val _managedDisplayInstances = linkedMapOf<UUID, DisplayInstance>()
    private val displayGeometryByInstanceId = HashMap<UUID, DisplayGeometry>()
    private val receivedMapIdsByPlayer = HashMap<UUID, MutableSet<Int>>()
    private val visibleTileIdsByPlayer = LinkedHashMap<UUID, LinkedHashSet<Int>>()
    private val decodedTilesByTileId = HashMap<Int, ByteArray>()

    init {
        validateSharedObjects(image, imageDataEntry)
        require(visibleDistance > 0.0) { "visibleDistance must be greater than 0" }
        require(updateInterval.isPositive()) { "updateInterval must be greater than 0" }
    }

    override fun attach(displayInstance: DisplayInstance) {
        check(!isStopped) { "DisplayJob is stopped" }
        validateDisplayInstance(displayInstance)

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

        val staticData = imageDataEntry.data
        val visibleTileIdsByPlayer = this.visibleTileIdsByPlayer.also { it.clear() }

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

                    val visibleRectByPlayer = geometry.computeVisibleTiles(
                        playerViews = playerViews,
                        visibleDistance = visibleDistance,
                    )

                    visibleRectByPlayer.forEach { (playerView, rect) ->
                        val visibleTileIds = visibleTileIdsByPlayer.computeIfAbsent(playerView.id) {
                            LinkedHashSet()
                        }
                        collectTileIds(rect, image.widthBlocks, visibleTileIds)
                    }
                }
            }

        val decodedTilesByTileId = this.decodedTilesByTileId.also { it.clear() }

        visibleTileIdsByPlayer.forEach { (playerId, visibleTileIds) ->
            val sendJob = displayManager.getLoadedSendJob(playerId) ?: return@forEach
            val receivedMapIds = receivedMapIdsByPlayer.computeIfAbsent(playerId) { HashSet() }

            visibleTileIds.forEach { tileId ->
                val mapId = image.tileMapIds[tileId]
                if (!receivedMapIds.add(mapId)) {
                    return@forEach
                }

                val mapColors = decodedTilesByTileId.getOrPut(tileId) {
                    val tilePoolIndex = staticData.tileIndexes[tileId].toInt() and 0xFFFF
                    TileDecoder.decode(staticData.tilePool.getTile(tilePoolIndex).toByteArray())
                }
                sendJob.enqueue(MapUpdate(mapId = mapId, mapColors = mapColors))
            }
        }

        if (!isStopped && _managedDisplayInstances.isNotEmpty()) {
            displayScheduler.scheduleAwakeAt(this, clock.instant().plus(updateInterval.toJavaDuration()))
        }
    }

    override fun stop() {
        if (isStopped) {
            return
        }

        isStopped = true
        _managedDisplayInstances.clear()
        displayGeometryByInstanceId.clear()
        receivedMapIdsByPlayer.clear()
        visibleTileIdsByPlayer.clear()
        decodedTilesByTileId.clear()
    }

    private fun validateDisplayInstance(displayInstance: DisplayInstance) {
        require(displayInstance.belongsTo == belongsTo) {
            "DisplayInstance belongsTo mismatch: expected=$belongsTo, actual=${displayInstance.belongsTo}"
        }
    }

    private fun validateSharedObjects(
        image: Image,
        imageDataEntry: ImageDataEntry.Static,
    ) {
        require(image.id == belongsTo) {
            "Image id mismatch: expected=$belongsTo, actual=${image.id}"
        }
        require(image.type == ImageType.STATIC) {
            "StaticDisplayJob requires static image, actual=${image.type}"
        }
        require(imageDataEntry.imageId == belongsTo) {
            "ImageDataEntry belongsTo mismatch: expected=$belongsTo, actual=${imageDataEntry.imageId}"
        }
        require(imageDataEntry.type == ImageType.STATIC) {
            "StaticDisplayJob requires static image data, actual=${imageDataEntry.type}"
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
