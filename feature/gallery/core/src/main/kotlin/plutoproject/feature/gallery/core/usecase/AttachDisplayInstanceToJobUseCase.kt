package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.DisplayJob
import plutoproject.feature.gallery.core.DisplayManager
import plutoproject.feature.gallery.core.DisplayInstance
import plutoproject.feature.gallery.core.Image
import plutoproject.feature.gallery.core.ImageDataEntry

class AttachDisplayInstanceToJobUseCase(
    private val displayManager: DisplayManager,
) {
    sealed class Result {
        data class Ok(val job: DisplayJob) : Result()
        data object JobNotStarted : Result()
    }

    fun execute(
        displayInstance: DisplayInstance,
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ): Result {
        val job = displayManager.getLoadedDisplayJob(displayInstance.belongsTo)
            ?: return Result.JobNotStarted

        job.attach(displayInstance, image, imageDataEntry)
        displayManager.bindDisplayInstanceToJob(displayInstance.id, displayInstance.belongsTo)
        return Result.Ok(job)
    }
}
