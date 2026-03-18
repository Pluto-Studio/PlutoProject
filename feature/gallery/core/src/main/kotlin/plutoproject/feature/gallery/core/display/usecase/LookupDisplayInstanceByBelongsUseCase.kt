package plutoproject.feature.gallery.core.display.usecase

import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayInstanceRepository
import plutoproject.feature.gallery.core.display.DisplayManager
import java.util.UUID

class LookupDisplayInstanceByBelongsUseCase(
    private val displayInstances: DisplayInstanceRepository,
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val displayInstances: List<DisplayInstance>) : Result()
    }

    suspend fun execute(belongsTo: UUID): Result {
        val found = displayManager.lookupDisplayInstancesByBelongsTo(belongsTo) { targetBelongsTo ->
            displayInstances.findByBelongsTo(targetBelongsTo)
        }
        return Result.Ok(found)
    }
}
