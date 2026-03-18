package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.DisplayJob
import plutoproject.feature.gallery.core.DisplayJobFactory
import plutoproject.feature.gallery.core.DisplayManager
import plutoproject.feature.gallery.core.DisplayScheduler
import plutoproject.feature.gallery.core.Image
import plutoproject.feature.gallery.core.ImageDataEntry
import plutoproject.feature.gallery.core.DisplayInstance
import java.time.Clock

class StartDisplayJobUseCase(
    private val clock: Clock,
    private val displayScheduler: DisplayScheduler,
    private val displayManager: DisplayManager,
    private val displayJobFactory: DisplayJobFactory,
) {
    sealed class Result {
        data class Ok(val job: DisplayJob) : Result()
        data class AlreadyStarted(val job: DisplayJob) : Result()
    }

    fun execute(
        displayInstance: DisplayInstance,
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ): Result {
        validateSharedObjects(displayInstance, image, imageDataEntry)

        val existed = displayManager.getLoadedDisplayJob(displayInstance.belongsTo)
        if (existed != null) {
            return Result.AlreadyStarted(existed)
        }

        val job = displayJobFactory.create(image, imageDataEntry)
        displayManager.registerDisplayJob(job)
        job.attach(displayInstance, image, imageDataEntry)
        displayManager.bindDisplayInstanceToJob(displayInstance.id, displayInstance.belongsTo)
        displayScheduler.scheduleAwakeAt(job, clock.instant())
        return Result.Ok(job)
    }

    private fun validateSharedObjects(
        displayInstance: DisplayInstance,
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ) {
        require(displayInstance.belongsTo == image.id) {
            "DisplayInstance belongsTo mismatch: display.belongsTo=${displayInstance.belongsTo}, image.id=${image.id}"
        }
        require(image.id == imageDataEntry.belongsTo) {
            "ImageDataEntry belongsTo mismatch: image.id=${image.id}, belongsTo=${imageDataEntry.belongsTo}"
        }
        require(image.type == imageDataEntry.type) {
            "Image and ImageDataEntry type mismatch: image.type=${image.type}, entry.type=${imageDataEntry.type}"
        }
    }
}
