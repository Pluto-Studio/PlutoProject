package plutoproject.feature.gallery.core.render

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import plutoproject.feature.gallery.core.StaticImageData
import plutoproject.feature.gallery.core.render.geometry.calcTargetResolution
import plutoproject.feature.gallery.core.render.geometry.repositionerOf
import plutoproject.feature.gallery.core.render.geometry.scalerOf
import plutoproject.feature.gallery.core.render.mapcolor.AlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.MapColorQuantizer
import plutoproject.feature.gallery.core.render.tile.TileDeduper
import plutoproject.feature.gallery.core.render.tile.TileSplitter
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 静态图渲染默认实现：几何 -> Alpha 合成 -> MapColor 量化 -> Tile 切分与去重。
 */
class DefaultStaticImageRenderer(
    private val alphaCompositor: AlphaCompositor,
    private val mapColorQuantizer: MapColorQuantizer,
    private val logger: Logger,
) : StaticImageRenderer {
    override suspend fun render(request: RenderStaticImageRequest): RenderResult<StaticImageData> = try {
        checkpoint()

        val targetResolution = calcTargetResolution(request.mapXBlocks, request.mapYBlocks)
        checkpoint()

        val transform = repositionerOf(request.profile.repositionMode).reposition(
            sourceWidth = request.sourceImage.width,
            sourceHeight = request.sourceImage.height,
            destinationWidth = targetResolution.width,
            destinationHeight = targetResolution.height,
        )
        checkpoint()

        val scaledImage = scalerOf(request.profile.scaleAlgorithm).scale(request.sourceImage, transform)
        checkpoint()

        val composited = alphaCompositor.composite(scaledImage, request.profile.alphaBackgroundColorRgb)
        checkpoint()

        val mapColorPixels = mapColorQuantizer.quantize(composited, request.profile.ditherAlgorithm)
        checkpoint()

        val deduper = TileDeduper()
        val splitResult = TileSplitter.splitAndDedupe(
            mapColorPixels = mapColorPixels,
            width = targetResolution.width,
            height = targetResolution.height,
            mapXBlocks = request.mapXBlocks,
            mapYBlocks = request.mapYBlocks,
            deduper = deduper,
        )
        checkpoint()

        if (splitResult.status != RenderStatus.SUCCEED) {
            RenderResult.failed(splitResult.status)
        } else {
            RenderResult.succeed(
                StaticImageData(
                    tilePool = deduper.buildTilePool(),
                    tileIndexes = splitResult.tileIndexes!!,
                )
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.log(
            Level.WARNING,
            "Static image render pipeline failed with internal error: mapXBlocks=${request.mapXBlocks}, mapYBlocks=${request.mapYBlocks}",
            e,
        )
        RenderResult.failed(RenderStatus.PIPELINE_FAILED)
    }
}

private suspend fun checkpoint() {
    currentCoroutineContext().ensureActive()
}
