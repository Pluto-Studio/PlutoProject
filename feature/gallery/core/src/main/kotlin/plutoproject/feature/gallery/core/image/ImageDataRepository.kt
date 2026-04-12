package plutoproject.feature.gallery.core.image

import java.util.UUID

interface ImageDataRepository {
    suspend fun findByImageId(imageId: UUID): ImageData?

    suspend fun findByImageIds(imageIds: Collection<UUID>): Map<UUID, ImageData>

    suspend fun save(imageId: UUID, data: ImageData)

    suspend fun update(imageId: UUID, data: ImageData): Boolean

    suspend fun deleteByImageId(imageId: UUID)
}
