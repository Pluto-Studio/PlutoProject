package plutoproject.feature.gallery.core

import java.util.UUID

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
        mapWidthBlocks = 2,
        mapHeightBlocks = 2,
        tileMapIds = intArrayOf(100, 101, 102, 103),
    )
}

internal fun sampleDisplayInstance(
    id: UUID = dummyUuid(901),
    belongsTo: UUID = dummyUuid(902),
    chunkX: Int = 10,
    chunkZ: Int = 20,
): DisplayInstance {
    return DisplayInstance(
        id = id,
        belongsTo = belongsTo,
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

internal fun sampleStaticImageDataEntry(belongsTo: UUID = dummyUuid(907)): ImageDataEntry<*> {
    return ImageDataEntry(
        belongsTo = belongsTo,
        type = ImageType.STATIC,
        data = StaticImageData(
            tilePool = TilePool(
                offsets = intArrayOf(0, 0),
                blob = ByteArray(0),
            ),
            tileIndexes = shortArrayOf(0),
        ),
    )
}

internal class InMemoryImageRepository(
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

    override suspend fun save(image: Image) {
        storage[image.id] = image
    }

    override suspend fun deleteById(id: UUID) {
        storage.remove(id)
    }
}

internal class InMemoryImageDataEntryRepository(
    private val storage: MutableMap<UUID, ImageDataEntry<*>> = mutableMapOf(),
) : ImageDataEntryRepository {
    override suspend fun findByBelongsTo(belongsTo: UUID): ImageDataEntry<*>? {
        return storage[belongsTo]
    }

    override suspend fun findByBelongsToIn(belongsToList: Collection<UUID>): Map<UUID, ImageDataEntry<*>> {
        return belongsToList.mapNotNull { belongsTo -> storage[belongsTo]?.let { belongsTo to it } }.toMap()
    }

    override suspend fun save(entry: ImageDataEntry<*>) {
        storage[entry.belongsTo] = entry
    }

    override suspend fun deleteByBelongsTo(belongsTo: UUID) {
        storage.remove(belongsTo)
    }
}

internal open class InMemoryDisplayInstanceRepository(
    private val storage: MutableMap<UUID, DisplayInstance> = mutableMapOf(),
) : DisplayInstanceRepository {
    override suspend fun findById(id: UUID): DisplayInstance? {
        return storage[id]
    }

    override suspend fun findByIds(ids: Collection<UUID>): Map<UUID, DisplayInstance> {
        return ids.mapNotNull { id -> storage[id]?.let { id to it } }.toMap()
    }

    override suspend fun findByBelongsTo(belongsTo: UUID): List<DisplayInstance> {
        return storage.values.filter { it.belongsTo == belongsTo }
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

internal class InMemorySystemInformationRepository(
    initialLastAllocatedId: Int? = null,
) : SystemInformationRepository {
    var lastAllocatedId: Int? = initialLastAllocatedId
        private set

    override suspend fun allocateMapIds(count: Int, allocationRange: AllocationRange): Int? {
        val current = lastAllocatedId ?: (allocationRange.start - 1)
        val next = current + count
        if (next > allocationRange.end) {
            return null
        }
        lastAllocatedId = next
        return next
    }
}
