package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.ImageDataEntryRepository
import java.util.UUID

class DeleteImageDataEntryUseCase(
    private val entries: ImageDataEntryRepository,
) {
    sealed class Result {
        object Ok : Result()
    }

    suspend fun execute(belongsTo: UUID): Result {
        entries.deleteByBelongsTo(belongsTo)
        return Result.Ok
    }
}
