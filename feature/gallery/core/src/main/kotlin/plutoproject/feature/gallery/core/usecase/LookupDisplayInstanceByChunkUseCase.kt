package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.DisplayInstance
import plutoproject.feature.gallery.core.DisplayInstanceRepository
import plutoproject.feature.gallery.core.DisplayManager

class LookupDisplayInstanceByChunkUseCase(
    private val displayInstances: DisplayInstanceRepository,
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val displayInstances: List<DisplayInstance>) : Result()
    }

    suspend fun execute(chunkX: Int, chunkZ: Int): Result {
        val found = displayManager.lookupDisplayInstancesByChunk(chunkX, chunkZ) { x, z ->
            displayInstances.findByChunk(x, z)
        }
        return Result.Ok(found)
    }
}
