package plutoproject.feature.gallery.core.display.usecase

import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.job.DisplayJob
import plutoproject.feature.gallery.core.display.DisplayManager
import java.util.UUID

class DetachDisplayInstanceFromJobUseCase(
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(
            val job: DisplayJob,
            val detachedDisplayInstance: DisplayInstance?,
        ) : Result()

        data object JobNotStarted : Result()
    }

    fun execute(displayInstanceId: UUID): Result {
        val belongsTo = displayManager.getJobBelongsToByDisplayInstanceId(displayInstanceId)
            ?: return Result.JobNotStarted
        val job = displayManager.getLoadedDisplayJob(belongsTo)
            ?: return Result.JobNotStarted

        val detached = job.detach(displayInstanceId)
        displayManager.unbindDisplayInstanceFromJob(displayInstanceId)
        return Result.Ok(job, detached)
    }
}
