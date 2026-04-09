package plutoproject.feature.gallery.core.display

import java.util.UUID

interface DisplayInstanceRepository {
    suspend fun findById(id: UUID): DisplayInstance?

    suspend fun findByIds(ids: Collection<UUID>): Map<UUID, DisplayInstance>

    suspend fun findByImageId(imageId: UUID): List<DisplayInstance>

    suspend fun findByChunk(chunkX: Int, chunkZ: Int): List<DisplayInstance>

    suspend fun save(displayInstance: DisplayInstance)

    suspend fun deleteById(id: UUID)
}
