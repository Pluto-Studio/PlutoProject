package plutoproject.feature.gallery.core.display.usecase

import plutoproject.feature.gallery.core.display.job.DisplayJob
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry

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
        require(displayInstance.belongsTo == image.id) {
            "DisplayInstance belongsTo mismatch: display.belongsTo=${displayInstance.belongsTo}, image.id=${image.id}"
        }
        require(image.id == imageDataEntry.imageId) {
            "ImageDataEntry belongsTo mismatch: image.id=${image.id}, belongsTo=${imageDataEntry.imageId}"
        }
        require(image.type == imageDataEntry.type) {
            "Image and ImageDataEntry type mismatch: image.type=${image.type}, entry.type=${imageDataEntry.type}"
        }

        val job = displayManager.getLoadedDisplayJob(displayInstance.belongsTo)
            ?: return Result.JobNotStarted

        job.attach(displayInstance)
        displayManager.bindDisplayInstanceToJob(displayInstance.id, displayInstance.belongsTo)
        return Result.Ok(job)
    }
}
