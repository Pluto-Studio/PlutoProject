package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.StaticImageData
import plutoproject.feature.gallery.core.render.RenderResult
import plutoproject.feature.gallery.core.render.RenderStaticImageRequest
import plutoproject.feature.gallery.core.render.RenderStatus
import plutoproject.feature.gallery.core.render.StaticImageRenderer

class RenderStaticImageUseCase(
    private val renderer: StaticImageRenderer,
) {
    suspend fun execute(request: RenderStaticImageRequest): RenderResult<StaticImageData> {
        val tileCountValidation = validateTileCount(request.mapXBlocks, request.mapYBlocks)
        val tileCount = tileCountValidation.tileCount
            ?: return RenderResult.failed(tileCountValidation.status!!)

        val result = renderer.render(request)
        if (result.status != RenderStatus.SUCCEED) {
            return RenderResult.failed(result.status)
        }

        val imageData = result.imageData
            ?: return RenderResult.failed(RenderStatus.INCONSISTENT_RENDER_RESULT)

        val uniqueTileCount = uniqueTileCount(imageData.tilePool)
            ?: return RenderResult.failed(RenderStatus.INCONSISTENT_RENDER_RESULT)
        if (uniqueTileCount > MAX_UNIQUE_TILE_COUNT) {
            return RenderResult.failed(RenderStatus.UNIQUE_TILE_OVERFLOW)
        }

        if (imageData.tileIndexes.size != tileCount) {
            return RenderResult.failed(RenderStatus.TILE_INDEXES_LENGTH_MISMATCH)
        }

        return RenderResult.succeed(imageData)
    }
}
