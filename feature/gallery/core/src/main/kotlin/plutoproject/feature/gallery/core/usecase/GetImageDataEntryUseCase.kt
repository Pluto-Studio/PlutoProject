package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.ImageDataEntry
import plutoproject.feature.gallery.core.ImageDataEntryRepository
import plutoproject.feature.gallery.core.ImageManager
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
