package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.Image
import plutoproject.feature.gallery.core.ImageManager
import plutoproject.feature.gallery.core.ImageRepository
import java.util.UUID

class GetImagesByIdsUseCase(
    private val images: ImageRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        data class Ok(val images: Map<UUID, Image>) : Result()
    }

    suspend fun execute(ids: Collection<UUID>): Result {
        if (ids.isEmpty()) {
            return Result.Ok(emptyMap())
        }

        val loaded = imageManager.getLoadedImages(ids)
        val missingIds = ids.filterNot(loaded::containsKey)
        val loadedFromStorage = images.findByIds(missingIds)
        imageManager.loadImages(loadedFromStorage.values)
        return Result.Ok(loaded + loadedFromStorage)
    }
}
