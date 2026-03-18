package plutoproject.feature.gallery.core.display.usecase

import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayInstanceRepository
import plutoproject.feature.gallery.core.display.DisplayManager
import java.util.UUID

class GetDisplayInstancesByIdsUseCase(
    private val displayInstances: DisplayInstanceRepository,
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val displayInstances: Map<UUID, DisplayInstance>) : Result()
    }

    suspend fun execute(ids: Collection<UUID>): Result {
        if (ids.isEmpty()) {
            return Result.Ok(emptyMap())
        }

        val loaded = displayManager.getLoadedDisplayInstances(ids)
        val missingIds = ids.filterNot(loaded::containsKey)
        val loadedFromStorage = displayInstances.findByIds(missingIds)
        displayManager.loadDisplayInstances(loadedFromStorage.values)
        return Result.Ok(loaded + loadedFromStorage)
    }
}
