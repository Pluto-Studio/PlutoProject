package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.TileRect
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.core.display.DisplayGeometry
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.render.tile.codec.decodeTile
import java.time.Clock
import java.util.UUID

class StaticDisplayJob(
    override val belongsTo: UUID,
    private val displayScheduler: DisplayScheduler,
    private val viewPort: ViewPort,
    private val displayManager: DisplayManager,
    private val clock: Clock,
    private val visibleDistance: Double = DEFAULT_VISIBLE_DISTANCE,
    private val horizontalFovRadian: Double = DEFAULT_HORIZONTAL_FOV_RADIAN,
    private val verticalFovRadian: Double = DEFAULT_VERTICAL_FOV_RADIAN,
    private val updateIntervalMs: Long = DEFAULT_UPDATE_INTERVAL_MS,
) : DisplayJob {
    override var isStopped: Boolean = false
        private set

    override val managedDisplayInstances
        get() = _managedDisplayInstances

    private val _managedDisplayInstances = linkedMapOf<UUID, DisplayInstance>()
    private val displayGeometryByInstanceId = HashMap<UUID, DisplayGeometry>()
    private val receivedMapIdsByPlayer = HashMap<UUID, MutableSet<Int>>()

    private var image: Image? = null
    private var imageDataEntry: ImageDataEntry<*>? = null

    init {
        require(visibleDistance > 0.0) { "visibleDistance must be greater than 0" }
        require(updateIntervalMs > 0L) { "updateIntervalMs must be greater than 0" }
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

        val image = image ?: return
        val staticData = imageDataEntry?.asStaticData() ?: return
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

                    val visibleRectByPlayer = geometry.computeVisibleTiles(
                        playerViews = playerViews,
                        visibleDistance = visibleDistance,
                        horizontalFovRadian = horizontalFovRadian,
                        verticalFovRadian = verticalFovRadian,
                    )

                    visibleRectByPlayer.forEach { (playerView, rect) ->
                        val visibleTileIds = visibleTileIdsByPlayer.computeIfAbsent(playerView.id) {
                            LinkedHashSet()
                        }
                        collectTileIds(rect, image.mapWidthBlocks, visibleTileIds)
                    }
                }
            }

        val decodedTilesByTileId = HashMap<Int, ByteArray>()

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
                    decodeTile(staticData.tilePool.extractTileData(tilePoolIndex))
                }
                sendJob.enqueue(MapUpdate(mapId = mapId, mapColors = mapColors))
            }
        }

        if (!isStopped && _managedDisplayInstances.isNotEmpty()) {
            displayScheduler.scheduleAwakeAt(this, clock.instant().plusMillis(updateIntervalMs))
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
        image = null
        imageDataEntry = null
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
        require(image.type == ImageType.STATIC) {
            "StaticDisplayJob requires static image, actual=${image.type}"
        }
        require(imageDataEntry.belongsTo == belongsTo) {
            "ImageDataEntry belongsTo mismatch: expected=$belongsTo, actual=${imageDataEntry.belongsTo}"
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

    companion object {
        const val DEFAULT_VISIBLE_DISTANCE: Double = 64.0
        const val DEFAULT_HORIZONTAL_FOV_RADIAN: Double = Math.PI / 2.0
        const val DEFAULT_VERTICAL_FOV_RADIAN: Double = Math.PI / 2.0
        const val DEFAULT_UPDATE_INTERVAL_MS: Long = 50L
    }
}
