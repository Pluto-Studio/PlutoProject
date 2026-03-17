package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.DisplayInstance
import plutoproject.feature.gallery.core.DisplayInstanceRepository
import plutoproject.feature.gallery.core.DisplayManager
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
