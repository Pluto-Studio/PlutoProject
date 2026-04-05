package plutoproject.feature.gallery.core.display

import plutoproject.feature.gallery.core.display.job.DisplayJob
import plutoproject.feature.gallery.core.display.job.DisplayJobFactory
import plutoproject.feature.gallery.core.display.job.DisplayResourceFactory
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageData
import java.util.UUID

sealed interface ReplaceRuntimeImageDataResult {
    data class Updated(val job: DisplayJob) : ReplaceRuntimeImageDataResult
    data object NotRunning : ReplaceRuntimeImageDataResult
}

sealed interface StopDisplayRuntimeResult {
    data class Stopped(val job: DisplayJob) : StopDisplayRuntimeResult
    data object NotRunning : StopDisplayRuntimeResult
}

class DisplayRuntimeRegistry(
    private val displayResourceFactory: DisplayResourceFactory,
    private val displayJobFactory: DisplayJobFactory,
) {
    private val lock = Any()
    private val jobsByImageId = HashMap<UUID, DisplayJob>()

    fun getJob(imageId: UUID): DisplayJob? {
        return synchronized(lock) {
            jobsByImageId[imageId]
        }
    }

    fun attach(
        image: Image,
        data: ImageData,
        instance: DisplayInstance,
    ): DisplayJob {
        require(image.id == instance.imageId) {
            "DisplayInstance imageId mismatch: expected=${image.id}, actual=${instance.imageId}"
        }
        require(image.type == data.type) {
            "Image and ImageData type mismatch: image.type=${image.type}, data.type=${data.type}"
        }

        return synchronized(lock) {
            val job = jobsByImageId.getOrPut(image.id) {
                displayJobFactory.create(image, displayResourceFactory.create(data))
            }
            job.attach(instance)
            job
        }
    }

    fun detach(imageId: UUID, instanceId: UUID): DisplayInstance? {
        return synchronized(lock) {
            val job = jobsByImageId[imageId] ?: return null
            val detached = job.detach(instanceId) ?: return null
            if (job.isEmpty()) {
                jobsByImageId.remove(imageId)
                job.stop()
            }
            detached
        }
    }

    fun replaceImageData(imageId: UUID, newData: ImageData): ReplaceRuntimeImageDataResult {
        return synchronized(lock) {
            val job = jobsByImageId[imageId] ?: return ReplaceRuntimeImageDataResult.NotRunning
            job.replaceResource(displayResourceFactory.create(newData))
            ReplaceRuntimeImageDataResult.Updated(job)
        }
    }

    fun stop(imageId: UUID): StopDisplayRuntimeResult {
        return synchronized(lock) {
            val job = jobsByImageId.remove(imageId) ?: return StopDisplayRuntimeResult.NotRunning
            job.stop()
            StopDisplayRuntimeResult.Stopped(job)
        }
    }

    fun close() {
        val jobs = synchronized(lock) {
            jobsByImageId.values.toList().also { jobsByImageId.clear() }
        }
        jobs.forEach(DisplayJob::stop)
    }
}
