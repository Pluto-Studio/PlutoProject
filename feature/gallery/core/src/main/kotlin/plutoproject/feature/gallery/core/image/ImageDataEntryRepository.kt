package plutoproject.feature.gallery.core.image

import java.util.UUID

interface ImageDataEntryRepository {
    suspend fun findByImageId(imageId: UUID): ImageDataEntry<*>?

    suspend fun findByImageIds(imageIds: Collection<UUID>): Map<UUID, ImageDataEntry<*>>

    suspend fun save(entry: ImageDataEntry<*>)

    suspend fun deleteByImageId(belongsTo: UUID)
}
