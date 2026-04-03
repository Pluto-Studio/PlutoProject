package plutoproject.feature.gallery.core.image

import java.util.UUID

interface ImageRepository {
    suspend fun findById(id: UUID): Image?

    suspend fun findByIds(ids: Collection<UUID>): Map<UUID, Image>

    suspend fun findByOwner(owner: UUID): List<Image>

    suspend fun count(): Int

    suspend fun save(image: Image)

    suspend fun deleteById(id: UUID)
}
