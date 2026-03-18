package plutoproject.feature.gallery.core.display.usecase

import plutoproject.feature.gallery.core.display.DisplayInstanceRepository
import plutoproject.feature.gallery.core.display.DisplayManager
import java.util.UUID

class DeleteDisplayInstanceUseCase(
    private val displayInstances: DisplayInstanceRepository,
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        object Ok : Result()
        object NotExisted : Result()
    }

    suspend fun execute(id: UUID): Result = displayManager.withDisplayInstanceOperationLock(id) {
        if (displayManager.getLoadedDisplayInstance(id) == null && displayInstances.findById(id) == null) {
            return@withDisplayInstanceOperationLock Result.NotExisted
        }

        displayManager.unloadDisplayInstance(id)
        displayInstances.deleteById(id)
        Result.Ok
    }
}
