package plutoproject.feature.gallery.core

import java.util.UUID

interface ImageRepository {
    suspend fun findById(id: UUID): Image?

    suspend fun findByOwner(owner: UUID): List<Image>

    suspend fun save(image: Image)

    suspend fun deleteById(id: UUID)
}
