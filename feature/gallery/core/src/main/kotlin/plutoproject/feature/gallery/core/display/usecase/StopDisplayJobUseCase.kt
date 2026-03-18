package plutoproject.feature.gallery.core.display.usecase

import plutoproject.feature.gallery.core.display.job.DisplayJob
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
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
