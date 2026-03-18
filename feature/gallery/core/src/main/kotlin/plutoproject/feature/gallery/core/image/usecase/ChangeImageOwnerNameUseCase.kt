package plutoproject.feature.gallery.core.image.usecase

import plutoproject.feature.gallery.core.image.ImageManager
import plutoproject.feature.gallery.core.image.ImageRepository
import java.util.UUID

class ChangeImageOwnerNameUseCase(
    private val images: ImageRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        object Ok : Result()
        object NotFound : Result()
    }

    suspend fun execute(id: UUID, newOwnerName: String): Result {
        val loadedImage = imageManager.getLoadedImage(id)
        if (loadedImage != null) {
            loadedImage.changeOwnerName(newOwnerName)
            images.save(loadedImage)
            return Result.Ok
        }

        val image = images.findById(id) ?: return Result.NotFound
        image.changeOwnerName(newOwnerName)
        images.save(image)
        return Result.Ok
    }
}
