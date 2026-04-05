package plutoproject.feature.gallery.core.display

import java.util.UUID

class DisplayInstanceStore(
    private val instanceRepo: DisplayInstanceRepository,
) {
    suspend fun create(instance: DisplayInstance): Boolean {
        if (instanceRepo.findById(instance.id) != null) {
            return false
        }

        instanceRepo.save(instance)
        return true
    }

    suspend fun get(id: UUID): DisplayInstance? {
        return instanceRepo.findById(id)
    }

    suspend fun findByImageId(imageId: UUID): List<DisplayInstance> {
        return instanceRepo.findByImageId(imageId)
    }

    suspend fun findByChunk(chunkX: Int, chunkZ: Int): List<DisplayInstance> {
        return instanceRepo.findByChunk(chunkX, chunkZ)
    }

    suspend fun delete(id: UUID): DisplayInstance? {
        val instance = instanceRepo.findById(id) ?: return null
        instanceRepo.deleteById(id)
        return instance
    }
}
