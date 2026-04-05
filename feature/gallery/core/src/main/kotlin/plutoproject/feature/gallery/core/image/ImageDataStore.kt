package plutoproject.feature.gallery.core.image

import java.util.UUID

class ImageDataStore(
    private val imageDataRepo: ImageDataRepository,
) {
    suspend fun create(imageId: UUID, data: ImageData): Boolean {
        if (imageDataRepo.findByImageId(imageId) != null) {
            return false
        }

        imageDataRepo.save(imageId, data)
        return true
    }

    suspend fun get(imageId: UUID): ImageData? {
        return imageDataRepo.findByImageId(imageId)
    }

    suspend fun getMany(imageIds: Collection<UUID>): Map<UUID, ImageData> {
        return imageDataRepo.findByImageIds(imageIds)
    }

    suspend fun save(imageId: UUID, data: ImageData): Boolean {
        return imageDataRepo.update(imageId, data)
    }

    suspend fun delete(imageId: UUID): ImageData? {
        val data = imageDataRepo.findByImageId(imageId) ?: return null
        imageDataRepo.deleteByImageId(imageId)
        return data
    }
}
