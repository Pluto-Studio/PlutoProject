package plutoproject.feature.gallery.core.image.usecase

import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageDataEntryRepository
import plutoproject.feature.gallery.core.image.ImageManager
import plutoproject.feature.gallery.core.image.ImageType
import java.util.UUID

class CreateImageDataEntryUseCase(
    private val entries: ImageDataEntryRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        data class Ok(val entry: ImageDataEntry<*>) : Result()
        data class AlreadyExisted(val entry: ImageDataEntry<*>) : Result()
    }

    suspend fun <T : Any> execute(
        belongsTo: UUID,
        type: ImageType,
        data: T,
    ): Result {
        val existed = imageManager.getLoadedImageDataEntry(belongsTo)
            ?: entries.findByBelongsTo(belongsTo)
        if (existed != null) {
            return Result.AlreadyExisted(existed)
        }

        val entry = ImageDataEntry(
            belongsTo = belongsTo,
            type = type,
            data = data,
        )
        entries.save(entry)
        imageManager.loadImageDataEntry(entry)
        return Result.Ok(entry)
    }
}
