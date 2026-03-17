package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.ImageManager
import plutoproject.feature.gallery.core.ImageRepository
import java.util.UUID

class DeleteImageUseCase(
    private val images: ImageRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        object Ok : Result()
        object NotExisted : Result()
    }

    suspend fun execute(id: UUID): Result {
        if (imageManager.getLoadedImage(id) == null && images.findById(id) == null) {
            return Result.NotExisted
        }

        imageManager.unloadImage(id)
        images.deleteById(id)
        return Result.Ok
    }
}
