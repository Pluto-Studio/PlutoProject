package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.ImageDataEntry
import plutoproject.feature.gallery.core.ImageDataEntryRepository
import plutoproject.feature.gallery.core.ImageManager
import java.util.UUID

class ReplaceImageDataEntryUseCase(
    private val entries: ImageDataEntryRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        object Ok : Result()
        object NotFound : Result()
    }

    suspend fun <T : Any> execute(belongsTo: UUID, newData: T): Result {
        val loadedEntry = imageManager.getLoadedImageDataEntry(belongsTo)
        if (loadedEntry != null) {
            loadedEntry.replaceDataUnchecked(newData)
            entries.save(loadedEntry)
            return Result.Ok
        }

        val entry = entries.findByBelongsTo(belongsTo) ?: return Result.NotFound
        entry.replaceDataUnchecked(newData)
        entries.save(entry)
        return Result.Ok
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> ImageDataEntry<*>.replaceDataUnchecked(newData: T) {
        (this as ImageDataEntry<T>).replaceData(newData)
    }
}
