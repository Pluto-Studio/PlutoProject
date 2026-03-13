package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.ImageDataEntry
import plutoproject.feature.gallery.core.ImageDataEntryRepository
import plutoproject.feature.gallery.core.ImageManager
import plutoproject.feature.gallery.core.ImageType
import java.util.UUID

class CreateImageDataEntryUseCase(
    private val entries: ImageDataEntryRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        data class Ok(val entry: ImageDataEntry<*>) : Result()
    }

    suspend fun <T : Any> execute(
        belongsTo: UUID,
        type: ImageType,
        data: T,
    ): Result {
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
