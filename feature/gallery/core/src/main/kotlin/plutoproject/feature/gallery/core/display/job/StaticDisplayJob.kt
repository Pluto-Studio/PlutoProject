package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.display.*
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.render.tile.codec.TileDecoder
import plutoproject.feature.gallery.core.util.VisibleTileSet
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

    override val attachedDisplayInstances
        get() = _attachedDisplayInstances

    private val _attachedDisplayInstances = HashMap<UUID, DisplayInstance>()
    private val displayGeometryByInstanceId = HashMap<UUID, DisplayGeometry>()
    private val sentMapIdsByPlayer = HashMap<UUID, MutableSet<Int>>()
    private val maxTileCount = image.widthBlocks * image.heightBlocks

    // wake 内使用，每轮可能清理
    private val visibleTileIdsByPlayer = HashMap<UUID, VisibleTileSet>()
    private val activePlayerIds = HashSet<UUID>()
    private val decodedTilesByPoolIndex = HashMap<Int, ByteArray>()
    private val playerViewsByWorld = HashMap<String, List<PlayerView>>()

    private var cachedImageData: ImageData.Static = imageDataEntry.data

    init {
        validateSharedObjects(image, imageDataEntry)
        require(visibleDistance > 0.0) { "visibleDistance must be greater than 0" }
        require(updateInterval.isPositive()) { "updateInterval must be greater than 0" }
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

        playerViewsByWorld.clear()
        activePlayerIds.clear()
        visibleTileIdsByPlayer.values.forEach(VisibleTileSet::clear)

        collectVisibleTileIds()
        pruneInactivePlayerTileCaches()
        sendVisibleTiles(currentImageData())
        scheduleNextAwake()
    }

    private fun scheduleNextAwake() {
        if (isStopped || _attachedDisplayInstances.isEmpty()) {
            return
        }
        val nextAwake = clock.instant().plus(updateInterval.toJavaDuration())
        displayScheduler.scheduleAwakeAt(this, nextAwake)
    }

    private fun currentImageData(): ImageData.Static {
        val currentData = imageDataEntry.data
        if (cachedImageData !== currentData) {
            cachedImageData = currentData
            decodedTilesByPoolIndex.clear()
        }
        return currentData
    }

    private fun collectVisibleTileIds() {
        _attachedDisplayInstances.forEach { (_, instance) ->
            val views = playerViewsByWorld.getOrPut(instance.world) {
                viewPort.getPlayerViews(instance.world).also { views ->
                    views.forEach { activePlayerIds.add(it.id) }
                }
            }

            if (views.isEmpty()) {
                return@forEach
            }

            collectTileIdsForInstance(instance, views)
        }
    }

    private fun collectTileIdsForInstance(instance: DisplayInstance, views: List<PlayerView>) {
        val geometry = displayGeometryByInstanceId.getOrPut(instance.id) {
            instance.buildGeometry()
        }

        views.forEach { view ->
            val rect = geometry.computeVisibleTiles(view, visibleDistance) ?: return@forEach
            val visibleTileIds = visibleTileIdsByPlayer.getOrPut(view.id) {
                VisibleTileSet(maxTileCount)
            }
            collectTileIds(rect, image.widthBlocks, visibleTileIds)
        }
    }

    private fun pruneInactivePlayerTileCaches() {
        visibleTileIdsByPlayer.keys.removeIf { playerId -> playerId !in activePlayerIds }
    }

    private fun collectTileIds(rect: TileRect, mapWidthBlocks: Int, output: VisibleTileSet) {
        for (y in rect.minY..rect.maxY) {
            for (x in rect.minX..rect.maxX) {
                output.add(y * mapWidthBlocks + x)
            }
        }
    }

    private fun sendVisibleTiles(imageData: ImageData.Static) {
        visibleTileIdsByPlayer.forEach { (playerId, visibleTileIds) ->
            if (visibleTileIds.size == 0) {
                return@forEach
            }
            sendPlayerVisibleTiles(playerId, visibleTileIds, imageData)
        }
    }

    private fun sendPlayerVisibleTiles(
        playerId: UUID,
        visibleTileIds: VisibleTileSet,
        imageData: ImageData.Static,
    ) {
        val sendJob = displayManager.getLoadedSendJob(playerId) ?: return
        val sentMapIds = sentMapIdsByPlayer.getOrPut(playerId) { HashSet() }

        visibleTileIds.forEach { tileId ->
            val mapId = image.tileMapIds[tileId]
            if (!sentMapIds.add(mapId)) {
                return@forEach
            }

            val tilePoolIndex = imageData.tileIndexes[tileId].toInt() and 0xFFFF
            val mapColors = decodedTilesByPoolIndex.getOrPut(tilePoolIndex) {
                TileDecoder.decode(imageData.tilePool.getTile(tilePoolIndex).toByteArray())
            }
            sendJob.enqueue(MapUpdate(mapId, mapColors))
        }
    }

    // 玩家退出的时候调用
    fun removePlayerCache(player: UUID) {
        sentMapIdsByPlayer.remove(player)
        visibleTileIdsByPlayer.remove(player)
    }

    override fun stop() {
        if (isStopped) {
            return
        }

        isStopped = true

        _attachedDisplayInstances.clear()
        displayGeometryByInstanceId.clear()
        sentMapIdsByPlayer.clear()

        visibleTileIdsByPlayer.clear()
        activePlayerIds.clear()
        decodedTilesByPoolIndex.clear()
        playerViewsByWorld.clear()
    }
}
