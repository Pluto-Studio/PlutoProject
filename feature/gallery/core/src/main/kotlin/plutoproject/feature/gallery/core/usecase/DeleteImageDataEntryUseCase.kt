package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.ImageManager
import plutoproject.feature.gallery.core.ImageDataEntryRepository
import java.util.UUID

class DeleteImageDataEntryUseCase(
    private val entries: ImageDataEntryRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        object Ok : Result()
        object NotExisted : Result()
    }

    suspend fun execute(belongsTo: UUID): Result {
        if (imageManager.getLoadedImageDataEntry(belongsTo) == null && entries.findByBelongsTo(belongsTo) == null) {
            return Result.NotExisted
        }

        imageManager.unloadImageDataEntry(belongsTo)
        entries.deleteByBelongsTo(belongsTo)
        return Result.Ok
    }
}
