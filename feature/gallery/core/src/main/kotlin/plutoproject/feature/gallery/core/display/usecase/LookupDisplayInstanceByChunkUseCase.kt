package plutoproject.feature.gallery.core.display.usecase

import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayInstanceRepository
import plutoproject.feature.gallery.core.display.DisplayManager

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
