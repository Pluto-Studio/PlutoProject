package plutoproject.feature.gallery.core.display.usecase

import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.job.SendJob
import java.util.UUID

class StopSendJobUseCase(
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val job: SendJob) : Result()
        data object NotStarted : Result()
    }

    fun execute(playerId: UUID): Result {
        val job = displayManager.getLoadedSendJob(playerId) ?: return Result.NotStarted
        job.stop()
        displayManager.removeSendJob(playerId)
        return Result.Ok(job)
    }
}
