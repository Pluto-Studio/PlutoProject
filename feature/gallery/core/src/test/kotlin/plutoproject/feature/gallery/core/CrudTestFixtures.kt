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

internal class InMemoryImageRepository(
    private val storage: MutableMap<UUID, Image> = mutableMapOf(),
) : ImageRepository {
    override suspend fun findById(id: UUID): Image? {
        return storage[id]
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

    override suspend fun save(entry: ImageDataEntry<*>) {
        storage[entry.belongsTo] = entry
    }

    override suspend fun deleteByBelongsTo(belongsTo: UUID) {
        storage.remove(belongsTo)
    }
}
