package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.DisplayInstance
import plutoproject.feature.gallery.core.DisplayJob
import plutoproject.feature.gallery.core.DisplayManager
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
