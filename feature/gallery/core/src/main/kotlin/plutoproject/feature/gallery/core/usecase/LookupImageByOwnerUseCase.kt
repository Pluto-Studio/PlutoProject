package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.Image
import plutoproject.feature.gallery.core.ImageRepository
import java.util.UUID

class LookupImageByOwnerUseCase(
    private val images: ImageRepository,
) {
    sealed class Result {
        data class Ok(val images: List<Image>) : Result()
    }

    suspend fun execute(owner: UUID): Result {
        return Result.Ok(images.findByOwner(owner))
    }
}
