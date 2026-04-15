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
import java.util.HashMap
import java.util.HashSet
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class StaticDisplayJob(
    override val imageId: UUID,
    private val image: Image,
    initialResource: StaticDisplayResource,
    private val displayScheduler: DisplayScheduler,
    private val viewPort: ViewPort,
    private val sendJobRegistry: SendJobRegistry,
    private val clock: Clock,
    private val visibleDistance: Double,
    private val updateInterval: Duration,
) : DisplayJob {
    override val type: ImageType = ImageType.STATIC

    override val isStopped: Boolean
        get() = synchronized(lock) { stopped }

    override val attachedDisplayInstances: Map<UUID, DisplayInstance>
        get() = synchronized(lock) { attachedInstances.toMap() }

    private val lock = Any()
    private val attachedInstances = HashMap<UUID, DisplayInstance>()
    private val displayGeometryByInstanceId = HashMap<UUID, DisplayGeometry>()
    private val sentMapIdsByPlayer = HashMap<UUID, MutableSet<Int>>()
    private val visibleTileIdsByPlayer = HashMap<UUID, VisibleTileSet>()
    private val activePlayerIds = HashSet<UUID>()
    private val playerViewsByWorld = HashMap<String, List<PlayerView>>()
    private val maxTileCount = image.widthBlocks * image.heightBlocks

    private var stopped = false
    private var resource: StaticDisplayResource = initialResource
    private var resourceRevision: Long = 0

    init {
        require(image.id == imageId) {
            "Image id mismatch: expected=$imageId, actual=${image.id}"
        }
        require(image.type == ImageType.STATIC) {
            "StaticDisplayJob requires static image, actual=${image.type}"
        }
        require(visibleDistance > 0.0) { "visibleDistance must be greater than 0" }
        require(updateInterval.isPositive()) { "updateInterval must be greater than 0" }
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
        require(resource is StaticDisplayResource) {
            "StaticDisplayJob requires static display resource, actual=${resource::class.simpleName}"
        }

        synchronized(lock) {
            check(!stopped) { "DisplayJob is stopped" }
            this.resource = resource
            resourceRevision++
            sentMapIdsByPlayer.clear()
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

            playerViewsByWorld.clear()
            activePlayerIds.clear()
            visibleTileIdsByPlayer.values.forEach(VisibleTileSet::clear)

            collectVisibleTileIds()
            pruneInactivePlayerTileCaches()
            sendVisibleTiles(resource)
            scheduleNextAwake()
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
            sentMapIdsByPlayer.clear()
            visibleTileIdsByPlayer.clear()
            activePlayerIds.clear()
            playerViewsByWorld.clear()
        }
    }

    fun clearPlayerCache(player: UUID) {
        synchronized(lock) {
            sentMapIdsByPlayer.remove(player)
            visibleTileIdsByPlayer.remove(player)
        }
    }

    private fun scheduleNextAwake() {
        if (stopped || attachedInstances.isEmpty()) {
            return
        }
        val nextAwake = clock.instant().plus(updateInterval.toJavaDuration())
        displayScheduler.scheduleAwakeAt(this, nextAwake)
    }

    private fun collectVisibleTileIds() {
        attachedInstances.forEach { (_, instance) ->
            val views = playerViewsByWorld.getOrPut(instance.world) {
                viewPort.getPlayerViews(instance.world).also { currentViews ->
                    currentViews.forEach { activePlayerIds.add(it.id) }
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

    private fun sendVisibleTiles(resource: StaticDisplayResource) {
        visibleTileIdsByPlayer.forEach { (playerId, visibleTileIds) ->
            if (visibleTileIds.size == 0) {
                return@forEach
            }
            sendPlayerVisibleTiles(playerId, visibleTileIds, resource)
        }
    }

    private fun sendPlayerVisibleTiles(
        playerId: UUID,
        visibleTileIds: VisibleTileSet,
        resource: StaticDisplayResource,
    ) {
        val sendJob = sendJobRegistry.get(playerId) ?: return
        val sentMapIds = sentMapIdsByPlayer.getOrPut(playerId) { HashSet() }

        visibleTileIds.forEach { tileId ->
            val mapId = image.tileMapIds[tileId]
            if (!sentMapIds.add(mapId)) {
                return@forEach
            }

            val tilePoolIndex = resource.tileIndexes[tileId].toInt() and 0xFFFF
            sendJob.enqueue(
                SendRequest(
                    sourceId = imageId,
                    priority = SendPriority.STATIC,
                    update = MapUpdate(mapId, resource.decodedTilePool.getTile(tilePoolIndex)),
                    fingerprint = MapContentFingerprint(
                        resourceRevision = resourceRevision,
                        tileToken = tilePoolIndex,
                    ),
                )
            )
        }
    }
}
