package plutoproject.feature.gallery.core.image.usecase

import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageManager
import plutoproject.feature.gallery.core.image.ImageRepository
import plutoproject.feature.gallery.core.image.ImageType
import java.util.UUID

class CreateImageUseCase(
    private val images: ImageRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        data class Ok(val image: Image) : Result()
        data class AlreadyExisted(val image: Image) : Result()
    }

    suspend fun execute(
        id: UUID,
        type: ImageType,
        owner: UUID,
        ownerName: String,
        name: String,
        mapWidthBlocks: Int,
        mapHeightBlocks: Int,
        tileMapIds: IntArray,
    ): Result {
        val existed = imageManager.getLoadedImage(id)
            ?: images.findById(id)
        if (existed != null) {
            return Result.AlreadyExisted(existed)
        }

        val image = Image(
            id = id,
            type = type,
            owner = owner,
            ownerName = ownerName,
            name = name,
            mapWidthBlocks = mapWidthBlocks,
            mapHeightBlocks = mapHeightBlocks,
            tileMapIds = tileMapIds,
        )
        images.save(image)
        imageManager.loadImage(image)
        return Result.Ok(image)
    }
}
