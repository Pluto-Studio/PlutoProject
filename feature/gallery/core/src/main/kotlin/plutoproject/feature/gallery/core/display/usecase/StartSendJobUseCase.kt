package plutoproject.feature.gallery.core.display.usecase

import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.job.SendJob
import plutoproject.feature.gallery.core.display.job.SendJobFactory
import java.util.UUID

class StartSendJobUseCase(
    private val displayManager: DisplayManager,
    private val sendJobFactory: SendJobFactory,
) {
    sealed class Result {
        data class Ok(val job: SendJob) : Result()
        data class AlreadyStarted(val job: SendJob) : Result()
    }

    fun execute(playerId: UUID): Result {
        val existed = displayManager.getLoadedSendJob(playerId)
        if (existed != null) {
            return Result.AlreadyStarted(existed)
        }

        val job = sendJobFactory.create(playerId)
        displayManager.registerSendJob(job)
        return Result.Ok(job)
    }
}
