package plutoproject.feature.gallery.adapter.paper

import org.bukkit.Chunk
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.image.ImageManager
import plutoproject.feature.gallery.core.display.usecase.AttachDisplayInstanceToJobUseCase
import plutoproject.feature.gallery.core.display.usecase.DetachDisplayInstanceFromJobUseCase
import plutoproject.feature.gallery.core.display.usecase.GetDisplayInstancesByIdsUseCase
import plutoproject.feature.gallery.core.display.usecase.StartDisplayJobUseCase
import plutoproject.feature.gallery.core.display.usecase.StartSendJobUseCase
import plutoproject.feature.gallery.core.display.usecase.StopDisplayJobUseCase
import plutoproject.feature.gallery.core.display.usecase.StopSendJobUseCase
import plutoproject.feature.gallery.core.image.usecase.GetImageDataEntriesByBelongsToUseCase
import plutoproject.feature.gallery.core.image.usecase.GetImagesByIdsUseCase
import java.util.UUID

class GalleryRuntimeCoordinator(
    private val chunkDisplayIndexStorage: PaperChunkDisplayIndexStorage,
    private val displayManager: DisplayManager,
    private val imageManager: ImageManager,
    private val displayScheduler: DisplayScheduler,
    private val getDisplayInstancesByIds: GetDisplayInstancesByIdsUseCase,
    private val getImagesByIds: GetImagesByIdsUseCase,
    private val getImageDataEntriesByBelongsTo: GetImageDataEntriesByBelongsToUseCase,
    private val startDisplayJob: StartDisplayJobUseCase,
    private val stopDisplayJob: StopDisplayJobUseCase,
    private val attachDisplayInstanceToJob: AttachDisplayInstanceToJobUseCase,
    private val detachDisplayInstanceFromJob: DetachDisplayInstanceFromJobUseCase,
    private val startSendJob: StartSendJobUseCase,
    private val stopSendJob: StopSendJobUseCase,
) {
    suspend fun onChunkLoad(chunk: Chunk) {
        val displayInstanceIds = chunkDisplayIndexStorage.readDisplayInstanceIds(chunk)
        if (displayInstanceIds.isEmpty()) {
            return
        }

        val loadedDisplays =
            (getDisplayInstancesByIds.execute(displayInstanceIds) as GetDisplayInstancesByIdsUseCase.Result.Ok).displayInstances
        if (loadedDisplays.isEmpty()) {
            return
        }

        val belongsToIds = loadedDisplays.values.map { it.belongsTo }.distinct()
        val loadedImages = (getImagesByIds.execute(belongsToIds) as GetImagesByIdsUseCase.Result.Ok).images
        val loadedEntries =
            (getImageDataEntriesByBelongsTo.execute(belongsToIds) as GetImageDataEntriesByBelongsToUseCase.Result.Ok).entries

        loadedDisplays.values
            .groupBy { it.belongsTo }
            .forEach { (belongsTo, displayInstances) ->
                val image = loadedImages[belongsTo] ?: return@forEach
                val imageDataEntry = loadedEntries[belongsTo] ?: return@forEach
                val existedJob = displayManager.getLoadedDisplayJob(belongsTo)

                if (existedJob == null) {
                    val first = displayInstances.first()
                    startDisplayJob.execute(first, image, imageDataEntry)
                    displayInstances.drop(1).forEach { displayInstance ->
                        attachDisplayInstanceToJob.execute(displayInstance, image, imageDataEntry)
                    }
                    return@forEach
                }

                displayInstances.forEach { displayInstance ->
                    attachDisplayInstanceToJob.execute(displayInstance, image, imageDataEntry)
                }
            }
    }

    fun onChunkUnload(chunk: Chunk) {
        val displayInstanceIds = chunkDisplayIndexStorage.readDisplayInstanceIds(chunk)
        if (displayInstanceIds.isEmpty()) {
            return
        }

        val loadedDisplays = displayManager.getLoadedDisplayInstances(displayInstanceIds)
        val displayIdsByBelongsTo = displayInstanceIds
            .mapNotNull { displayInstanceId ->
                displayManager.getJobBelongsToByDisplayInstanceId(displayInstanceId)?.let { belongsTo ->
                    belongsTo to displayInstanceId
                }
            }.groupBy({ it.first }, { it.second })

        displayIdsByBelongsTo.forEach { (belongsTo, idsInChunk) ->
            val job = displayManager.getLoadedDisplayJob(belongsTo) ?: return@forEach
            val remainingManagedIds = job.managedDisplayInstances.keys - idsInChunk.toSet()
            if (remainingManagedIds.isEmpty()) {
                stopDisplayJob.execute(belongsTo)
            } else {
                idsInChunk.forEach(detachDisplayInstanceFromJob::execute)
            }
        }

        val belongsToIds = loadedDisplays.values.map { it.belongsTo }.distinct()
        displayManager.unloadDisplayInstances(loadedDisplays.keys)
        imageManager.unloadImages(belongsToIds)
        imageManager.unloadImageDataEntries(belongsToIds)
    }

    fun onPlayerJoin(playerId: UUID) {
        startSendJob.execute(playerId)
    }

    fun onPlayerQuit(playerId: UUID) {
        stopSendJob.execute(playerId)
    }

    fun onPluginDisable() {
        displayManager.getLoadedSendJobs().map { it.playerId }.forEach(stopSendJob::execute)
        displayManager.getLoadedDisplayJobs().map { it.belongsTo }.forEach(stopDisplayJob::execute)
        displayScheduler.stop()
    }
}
