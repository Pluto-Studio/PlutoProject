package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.Image
import plutoproject.feature.gallery.core.ImageManager
import plutoproject.feature.gallery.core.ImageRepository
import plutoproject.feature.gallery.core.ImageType
import java.util.UUID

class CreateImageUseCase(
    private val images: ImageRepository,
    private val imageManager: ImageManager,
) {
    sealed class Result {
        data class Ok(val image: Image) : Result()
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
