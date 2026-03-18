package plutoproject.feature.gallery.core.image.usecase

import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageDataEntryRepository
import plutoproject.feature.gallery.core.image.ImageManager
import java.util.UUID

class GetImageDataEntriesByBelongsToUseCase(
    private val entries: ImageDataEntryRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        data class Ok(val entries: Map<UUID, ImageDataEntry<*>>) : Result()
    }

    suspend fun execute(belongsToList: Collection<UUID>): Result {
        if (belongsToList.isEmpty()) {
            return Result.Ok(emptyMap())
        }

        val loaded = imageManager.getLoadedImageDataEntries(belongsToList)
        val missingIds = belongsToList.filterNot(loaded::containsKey)
        val loadedFromStorage = entries.findByBelongsToIn(missingIds)
        imageManager.loadImageDataEntries(loadedFromStorage.values)
        return Result.Ok(loaded + loadedFromStorage)
    }
}
