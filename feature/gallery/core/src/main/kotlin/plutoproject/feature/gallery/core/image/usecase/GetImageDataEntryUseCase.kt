package plutoproject.feature.gallery.core.image.usecase

import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageDataEntryRepository
import plutoproject.feature.gallery.core.image.ImageManager
import java.util.UUID

class GetImageDataEntryUseCase(
    private val entries: ImageDataEntryRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        data class Ok(val entry: ImageDataEntry<*>?) : Result()
    }

    suspend fun execute(belongsTo: UUID): Result {
        val entry = imageManager.getImageDataEntry(belongsTo) { targetId ->
            entries.findByBelongsTo(targetId)
        }
        return Result.Ok(entry)
    }
}
