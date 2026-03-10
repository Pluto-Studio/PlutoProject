package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.AnimatedImageData
import plutoproject.feature.gallery.core.render.AnimatedImageRenderer
import plutoproject.feature.gallery.core.render.RenderAnimatedImageRequest
import plutoproject.feature.gallery.core.render.RenderResult
import plutoproject.feature.gallery.core.render.RenderStatus

class RenderAnimatedImageUseCase(
    private val renderer: AnimatedImageRenderer,
) {
    suspend fun execute(request: RenderAnimatedImageRequest): RenderResult<AnimatedImageData> {
        if (request.sourceFrames.isEmpty()) {
            return RenderResult.failed(RenderStatus.INVALID_SOURCE_FRAME_COUNT)
        }

        val tileCountValidation = validateTileCount(request.mapXBlocks, request.mapYBlocks)
        val singleFrameTileCount = tileCountValidation.tileCount
            ?: return RenderResult.failed(tileCountValidation.status!!)

        val result = renderer.render(request)
        if (result.status != RenderStatus.SUCCEED) {
            return RenderResult.failed(result.status)
        }

        val imageData = result.imageData
            ?: return RenderResult.failed(RenderStatus.INCONSISTENT_RENDER_RESULT)

        if (imageData.frameCount <= 0) {
            return RenderResult.failed(RenderStatus.INVALID_RENDERED_FRAME_COUNT)
        }
        if (imageData.durationMillis <= 0) {
            return RenderResult.failed(RenderStatus.INVALID_RENDERED_DURATION_MILLIS)
        }

        val uniqueTileCount = uniqueTileCount(imageData.tilePool)
            ?: return RenderResult.failed(RenderStatus.INCONSISTENT_RENDER_RESULT)
        if (uniqueTileCount > MAX_UNIQUE_TILE_COUNT) {
            return RenderResult.failed(RenderStatus.UNIQUE_TILE_OVERFLOW)
        }

        val expectedTileIndexesLength = singleFrameTileCount.toLong() * imageData.frameCount.toLong()
        if (expectedTileIndexesLength > Int.MAX_VALUE.toLong()) {
            return RenderResult.failed(RenderStatus.TILE_INDEXES_LENGTH_OVERFLOW)
        }
        if (imageData.tileIndexes.size != expectedTileIndexesLength.toInt()) {
            return RenderResult.failed(RenderStatus.TILE_INDEXES_LENGTH_MISMATCH)
        }

        return RenderResult.succeed(imageData)
    }
}
