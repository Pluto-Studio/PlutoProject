package plutoproject.feature.gallery.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayInstanceRepository
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.ItemFrameFacing
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.MapUpdatePort
import plutoproject.feature.gallery.core.display.PlayerView
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.core.display.job.DisplayJobFactory
import plutoproject.feature.gallery.core.display.job.SendJobFactory
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageManager
import plutoproject.feature.gallery.core.image.ImageDataEntryRepository
import plutoproject.feature.gallery.core.image.ImageRepository
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.render.tile.TilePool
import plutoproject.feature.gallery.core.render.tile.TilePoolSnapshot
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

internal fun dummyUuid(value: Long): UUID {
    return UUID(0L, value)
}

internal fun sampleImage(
    id: UUID = dummyUuid(1),
    owner: UUID = dummyUuid(2),
    ownerName: String = "Owner_123",
    name: String = "test-image",
): Image {
    return Image(
        id = id,
        type = ImageType.STATIC,
        owner = owner,
        ownerName = ownerName,
        name = name,
        widthBlocks = 2,
        heightBlocks = 2,
        tileMapIds = intArrayOf(100, 101, 102, 103),
    )
}

internal fun sampleDisplayInstance(
    id: UUID = dummyUuid(901),
    imageId: UUID = dummyUuid(902),
    chunkX: Int = 10,
    chunkZ: Int = 20,
): DisplayInstance {
    return DisplayInstance(
        id = id,
        imageId = imageId,
        world = "world",
        chunkX = chunkX,
        chunkZ = chunkZ,
        facing = ItemFrameFacing.NORTH,
        widthBlocks = 2,
        heightBlocks = 2,
        originX = 100.5,
        originY = 64.0,
        originZ = -32.25,
        itemFrameIds = listOf(dummyUuid(903), dummyUuid(904), dummyUuid(905), dummyUuid(906)),
    )
}

internal fun sampleStaticImageDataEntry(imageId: UUID = dummyUuid(907)): ImageDataEntry<*> {
    return ImageDataEntry.Static(
        imageId = imageId,
        data = ImageData.Static(
            tilePool = TilePool.fromSnapshot(
                TilePoolSnapshot(
                    offsets = intArrayOf(0, 0),
                    blob = ByteArray(0),
                )
            ),
            tileIndexes = shortArrayOf(0),
        ),
    )
}

internal open class InMemoryImageRepository(
    private val storage: MutableMap<UUID, Image> = mutableMapOf(),
) : ImageRepository {
    override suspend fun findById(id: UUID): Image? {
        return storage[id]
    }

    override suspend fun findByIds(ids: Collection<UUID>): Map<UUID, Image> {
        return ids.mapNotNull { id -> storage[id]?.let { id to it } }.toMap()
    }

    override suspend fun findByOwner(owner: UUID): List<Image> {
        return storage.values.filter { it.owner == owner }
    }

    override suspend fun count(): Int {
        return storage.size
    }

    override suspend fun save(image: Image) {
        storage[image.id] = image
    }

    override suspend fun deleteById(id: UUID) {
        storage.remove(id)
    }
}

internal open class InMemoryImageDataEntryRepository(
    private val storage: MutableMap<UUID, ImageDataEntry<*>> = mutableMapOf(),
) : ImageDataEntryRepository {
    override suspend fun findByImageId(imageId: UUID): ImageDataEntry<*>? {
        return storage[imageId]
    }

    override suspend fun findByImageIds(imageIds: Collection<UUID>): Map<UUID, ImageDataEntry<*>> {
        return imageIds.mapNotNull { belongsTo -> storage[belongsTo]?.let { belongsTo to it } }.toMap()
    }

    override suspend fun save(entry: ImageDataEntry<*>) {
        storage[entry.imageId] = entry
    }

    override suspend fun deleteByImageId(imageId: UUID) {
        storage.remove(imageId)
    }
}

internal open class InMemoryDisplayInstanceRepository(
    private val storage: MutableMap<UUID, DisplayInstance> = mutableMapOf(),
) : DisplayInstanceRepository {
    override suspend fun findById(id: UUID): DisplayInstance? {
        return storage[id]
    }

    override suspend fun findByImageId(imageId: UUID): List<DisplayInstance> {
        return storage.values.filter { it.imageId == imageId }
    }

    override suspend fun findByChunk(chunkX: Int, chunkZ: Int): List<DisplayInstance> {
        return storage.values.filter { it.chunkX == chunkX && it.chunkZ == chunkZ }
    }

    override suspend fun save(displayInstance: DisplayInstance) {
        storage[displayInstance.id] = displayInstance
    }

    override suspend fun deleteById(id: UUID) {
        storage.remove(id)
    }
}

internal fun newImageManager(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    clock: Clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
    imageRepo: ImageRepository = InMemoryImageRepository(),
    imageDataRepo: ImageDataEntryRepository = InMemoryImageDataEntryRepository(),
): ImageManager {
    return ImageManager(
        coroutineScope = scope,
        coroutineContext = coroutineContext,
        clock = clock,
        logger = Logger.getLogger("ImageManagerTest"),
        imageRepo = imageRepo,
        imageDataRepo = imageDataRepo,
    )
}

internal data class DisplayRuntimeFixture(
    val manager: DisplayManager,
    val scheduler: DisplayScheduler,
)

internal fun newDisplayRuntime(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    clock: Clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
    instances: DisplayInstanceRepository = InMemoryDisplayInstanceRepository(),
    schedulerScope: CoroutineScope = scope,
    schedulerContext: CoroutineContext = coroutineContext,
    awakeContext: CoroutineContext = coroutineContext,
    sendScope: CoroutineScope = scope,
    sendLoopContext: CoroutineContext = coroutineContext,
    mapUpdatePort: MapUpdatePort = object : MapUpdatePort {
        override fun send(playerId: UUID, update: MapUpdate) = Unit
    },
    viewPort: ViewPort = object : ViewPort {
        override fun getPlayerViews(world: String): List<PlayerView> = emptyList()
    },
): DisplayRuntimeFixture {
    val scheduler = DisplayScheduler(
        clock = clock,
        coroutineScope = schedulerScope,
        schedulerContext = schedulerContext,
        awakeContext = awakeContext,
    )
    lateinit var manager: DisplayManager
    val sendJobFactory = SendJobFactory(
        clock = clock,
        coroutineScope = sendScope,
        loopContext = sendLoopContext,
        mapUpdatePort = mapUpdatePort,
        maxQueueSize = 8,
        maxUpdatesInSpan = 8,
        updateLimitSpan = 1.seconds,
    )
    val displayJobFactory = lazy {
        DisplayJobFactory(
            displayScheduler = scheduler,
            viewPort = viewPort,
            displayManager = manager,
            clock = clock,
            animatedMaxFramesPerSecond = 20,
            visibleDistance = 5.0,
            staticUpdateInterval = 1.seconds,
        )
    }
    manager = DisplayManager(
        coroutineScope = scope,
        coroutineContext = coroutineContext,
        clock = clock,
        logger = Logger.getLogger("DisplayManagerTest"),
        instanceRepo = instances,
        scheduler = scheduler,
        displayJobFactory = displayJobFactory,
        sendJobFactory = lazy { sendJobFactory },
    )
    return DisplayRuntimeFixture(manager, scheduler)
}

internal fun TestScope.newDisplayRuntime(
    clock: Clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
    instances: DisplayInstanceRepository = InMemoryDisplayInstanceRepository(),
    mapUpdatePort: MapUpdatePort = object : MapUpdatePort {
        override fun send(playerId: UUID, update: MapUpdate) = Unit
    },
    viewPort: ViewPort = object : ViewPort {
        override fun getPlayerViews(world: String): List<PlayerView> = emptyList()
    },
): DisplayRuntimeFixture {
    return newDisplayRuntime(
        scope = this,
        coroutineContext = StandardTestDispatcher(testScheduler),
        clock = clock,
        instances = instances,
        schedulerScope = this,
        schedulerContext = StandardTestDispatcher(testScheduler),
        awakeContext = StandardTestDispatcher(testScheduler),
        sendScope = this,
        sendLoopContext = StandardTestDispatcher(testScheduler),
        mapUpdatePort = mapUpdatePort,
        viewPort = viewPort,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.newImageManager(
    clock: Clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
    imageRepo: ImageRepository = InMemoryImageRepository(),
    imageDataRepo: ImageDataEntryRepository = InMemoryImageDataEntryRepository(),
): ImageManager {
    return newImageManager(
        scope = this,
        coroutineContext = StandardTestDispatcher(testScheduler),
        clock = clock,
        imageRepo = imageRepo,
        imageDataRepo = imageDataRepo,
    )
}

internal fun schedulerClock(scope: TestScope): Clock {
    return object : Clock() {
        override fun instant(): Instant = Instant.ofEpochMilli(scope.testScheduler.currentTime)

        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?): Clock = this
    }
}

internal class InMemorySystemInformationRepository(
    initialLastAllocatedId: Int? = null,
) : SystemInformationRepository {
    var lastAllocatedId: Int? = initialLastAllocatedId
        private set

    override suspend fun allocateMapIds(count: Int, mapIdRange: MapIdRange): Int? {
        val current = lastAllocatedId ?: (mapIdRange.start - 1)
        val next = current + count
        if (next > mapIdRange.end) {
            return null
        }
        lastAllocatedId = next
        return next
    }
}
