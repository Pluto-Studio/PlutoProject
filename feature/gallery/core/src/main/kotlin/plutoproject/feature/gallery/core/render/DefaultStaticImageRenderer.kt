package plutoproject.feature.gallery.core.render

import plutoproject.feature.gallery.core.StaticImageData
import plutoproject.feature.gallery.core.render.geometry.calcTargetResolution
import plutoproject.feature.gallery.core.render.geometry.repositionerOf
import plutoproject.feature.gallery.core.render.geometry.scalerOf
import plutoproject.feature.gallery.core.render.mapcolor.AlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.DefaultAlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.MapColorQuantizer
import plutoproject.feature.gallery.core.render.mapcolor.newDefaultMapColorQuantizer
import plutoproject.feature.gallery.core.render.tile.TileSplitter

/**
 * 静态图渲染默认实现：几何 -> Alpha 合成 -> MapColor 量化 -> Tile 切分与去重。
 */
internal class DefaultStaticImageRenderer(
    private val alphaCompositor: AlphaCompositor = DefaultAlphaCompositor,
    private val mapColorQuantizer: MapColorQuantizer = newDefaultMapColorQuantizer(),
) : StaticImageRenderer {
    override suspend fun render(request: RenderStaticImageRequest): RenderResult<StaticImageData> = try {
        val targetResolution = calcTargetResolution(request.mapXBlocks, request.mapYBlocks)

        val transform = repositionerOf(request.profile.repositionMode).reposition(
            sourceWidth = request.sourceImage.width,
            sourceHeight = request.sourceImage.height,
            destinationWidth = targetResolution.width,
            destinationHeight = targetResolution.height,
        )

        val scaledImage = scalerOf(request.profile.scaleAlgorithm).scale(request.sourceImage, transform)
        val composited = alphaCompositor.composite(scaledImage, request.profile.alphaBackgroundColorRgb)
        val mapColorPixels = mapColorQuantizer.quantize(composited, request.profile.ditherAlgorithm)

        val splitResult = TileSplitter.splitAndDedupe(
            mapColorPixels = mapColorPixels,
            width = targetResolution.width,
            height = targetResolution.height,
            mapXBlocks = request.mapXBlocks,
            mapYBlocks = request.mapYBlocks,
        )

        if (splitResult.status != RenderStatus.SUCCEED) {
            RenderResult.failed(splitResult.status)
        } else {
            RenderResult.succeed(splitResult.imageData!!)
        }
    } catch (_: Exception) {
        RenderResult.failed(RenderStatus.PIPELINE_FAILED)
    }
}
