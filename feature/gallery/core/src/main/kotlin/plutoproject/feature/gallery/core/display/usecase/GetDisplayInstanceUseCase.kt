package plutoproject.feature.gallery.core.display.usecase

import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayInstanceRepository
import plutoproject.feature.gallery.core.display.DisplayManager
import java.util.UUID

class GetDisplayInstanceUseCase(
    private val displayInstances: DisplayInstanceRepository,
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val displayInstance: DisplayInstance?) : Result()
    }

    suspend fun execute(id: UUID): Result {
        val displayInstance = displayManager.getDisplayInstance(id) { targetId ->
            displayInstances.findById(targetId)
        }
        return Result.Ok(displayInstance)
    }
}
