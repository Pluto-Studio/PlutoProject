package plutoproject.feature.gallery.core.image

import java.util.UUID

class ImageStore(
    private val imageRepo: ImageRepository,
) {
    suspend fun create(image: Image): Boolean {
        if (imageRepo.findById(image.id) != null) {
            return false
        }

        imageRepo.save(image)
        return true
    }

    suspend fun get(id: UUID): Image? {
        return imageRepo.findById(id)
    }

    suspend fun getMany(ids: Collection<UUID>): Map<UUID, Image> {
        return imageRepo.findByIds(ids)
    }

    suspend fun findByOwner(owner: UUID): List<Image> {
        return imageRepo.findByOwner(owner)
    }

    suspend fun count(): Int {
        return imageRepo.count()
    }

    suspend fun save(image: Image): Boolean {
        return imageRepo.update(image)
    }

    suspend fun delete(id: UUID): Image? {
        val image = imageRepo.findById(id) ?: return null
        imageRepo.deleteById(id)
        return image
    }
}
