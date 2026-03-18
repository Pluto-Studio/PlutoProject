package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.DisplayJob
import plutoproject.feature.gallery.core.DisplayManager
import plutoproject.feature.gallery.core.DisplayScheduler
import java.util.UUID

class StopDisplayJobUseCase(
    private val displayScheduler: DisplayScheduler,
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val job: DisplayJob) : Result()
        data object NotStarted : Result()
    }

    fun execute(belongsTo: UUID): Result {
        val job = displayManager.getLoadedDisplayJob(belongsTo) ?: return Result.NotStarted

        displayScheduler.unschedule(job)
        job.managedDisplayInstances.keys.forEach(displayManager::unbindDisplayInstanceFromJob)
        job.stop()
        displayManager.removeDisplayJob(belongsTo)
        return Result.Ok(job)
    }
}
