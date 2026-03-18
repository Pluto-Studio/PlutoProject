package plutoproject.feature.gallery.core.image.usecase

import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageManager
import plutoproject.feature.gallery.core.image.ImageRepository
import java.util.UUID

class GetImageUseCase(
    private val images: ImageRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        data class Ok(val image: Image?) : Result()
    }

    suspend fun execute(id: UUID): Result {
        val image = imageManager.getImage(id) { targetId ->
            images.findById(targetId)
        }
        return Result.Ok(image)
    }
}
