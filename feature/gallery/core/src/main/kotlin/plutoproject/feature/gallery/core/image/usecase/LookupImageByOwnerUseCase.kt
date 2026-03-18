package plutoproject.feature.gallery.core.image.usecase

import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageRepository
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
