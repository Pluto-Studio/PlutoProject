package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.ImageRepository
import java.util.UUID

class DeleteImageUseCase(
    private val images: ImageRepository,
) {
    sealed class Result {
        object Ok : Result()
    }

    suspend fun execute(id: UUID): Result {
        images.deleteById(id)
        return Result.Ok
    }
}
