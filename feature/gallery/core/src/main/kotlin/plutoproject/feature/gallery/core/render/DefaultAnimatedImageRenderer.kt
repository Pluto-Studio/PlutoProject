package plutoproject.feature.gallery.core.render

import java.util.logging.Level
import java.util.logging.Logger
import plutoproject.feature.gallery.core.AnimatedImageData
import plutoproject.feature.gallery.core.render.geometry.TargetResolution
import plutoproject.feature.gallery.core.render.geometry.calcTargetResolution
import plutoproject.feature.gallery.core.render.geometry.repositionerOf
import plutoproject.feature.gallery.core.render.geometry.scalerOf
import plutoproject.feature.gallery.core.render.mapcolor.AlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.DefaultAlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.MapColorQuantizer
import plutoproject.feature.gallery.core.render.mapcolor.newDefaultMapColorQuantizer
import plutoproject.feature.gallery.core.render.tile.TileDeduper
import plutoproject.feature.gallery.core.render.tile.TileSplitter

internal class DefaultAnimatedImageRenderer(
    private val frameSampler: FrameSampler = DefaultFrameSampler,
    private val alphaCompositor: AlphaCompositor = DefaultAlphaCompositor,
    private val mapColorQuantizer: MapColorQuantizer = newDefaultMapColorQuantizer(),
    private val logger: Logger = Logger.getLogger(DefaultAnimatedImageRenderer::class.java.name),
) : AnimatedImageRenderer {
    override suspend fun render(request: RenderAnimatedImageRequest): RenderResult<AnimatedImageData> = try {
        val frameSampleResult = frameSampler.sample(request.sourceFrames, request.profile)
        if (frameSampleResult.status != RenderStatus.SUCCEED) {
            return RenderResult.failed(frameSampleResult.status)
        }
        val outToSourceFrameIndex = frameSampleResult.outToSourceFrameIndex!!
        val durationMillis = frameSampleResult.durationMillis!!

        val targetResolution = calcTargetResolution(request.mapXBlocks, request.mapYBlocks)
        val singleFrameTileCount = request.mapXBlocks * request.mapYBlocks
        val totalTileIndexesLengthLong = singleFrameTileCount.toLong() *
                outToSourceFrameIndex.size.toLong()
        if (totalTileIndexesLengthLong > Int.MAX_VALUE.toLong()) {
            return RenderResult.failed(RenderStatus.TILE_INDEXES_LENGTH_OVERFLOW)
        }

        val allFrameTileIndexes = ShortArray(totalTileIndexesLengthLong.toInt())
        val deduper = TileDeduper()
        val renderedFrameCache = HashMap<Int, ShortArray>()

        var outFrameIndex = 0
        while (outFrameIndex < outToSourceFrameIndex.size) {
            val srcFrameIndex = outToSourceFrameIndex[outFrameIndex]
            val frameTileIndexes = renderedFrameCache[srcFrameIndex]
                ?: renderSourceFrameTileIndexes(
                    request = request,
                    sourceFrameIndex = srcFrameIndex,
                    targetResolution = targetResolution,
                    deduper = deduper,
                ).also { renderedFrameCache[srcFrameIndex] = it }

            frameTileIndexes.copyInto(
                destination = allFrameTileIndexes,
                destinationOffset = outFrameIndex * singleFrameTileCount,
            )
            outFrameIndex++
        }

        RenderResult.succeed(
            AnimatedImageData(
                frameCount = outToSourceFrameIndex.size,
                durationMillis = durationMillis,
                tilePool = deduper.buildTilePool(),
                tileIndexes = allFrameTileIndexes,
            )
        )
    } catch (e: RenderPipelineException) {
        RenderResult.failed(e.status)
    } catch (e: Exception) {
        logger.log(
            Level.SEVERE,
            "Animated image render pipeline failed with internal error: sourceFrameCount=${request.sourceFrames.size}, mapXBlocks=${request.mapXBlocks}, mapYBlocks=${request.mapYBlocks}",
            e,
        )
        RenderResult.failed(RenderStatus.PIPELINE_FAILED)
    }

    private fun renderSourceFrameTileIndexes(
        request: RenderAnimatedImageRequest,
        sourceFrameIndex: Int,
        targetResolution: TargetResolution,
        deduper: TileDeduper,
    ): ShortArray {
        val sourceImage = request.sourceFrames[sourceFrameIndex].image

        val transform = repositionerOf(request.profile.repositionMode).reposition(
            sourceWidth = sourceImage.width,
            sourceHeight = sourceImage.height,
            destinationWidth = targetResolution.width,
            destinationHeight = targetResolution.height,
        )
        val scaledImage = scalerOf(request.profile.scaleAlgorithm).scale(sourceImage, transform)
        val composited = alphaCompositor.composite(scaledImage, request.profile.alphaBackgroundColorRgb)
        val mapColorPixels = mapColorQuantizer.quantize(composited, request.profile.ditherAlgorithm)

        val splitResult = TileSplitter.splitAndDedupe(
            mapColorPixels = mapColorPixels,
            width = targetResolution.width,
            height = targetResolution.height,
            mapXBlocks = request.mapXBlocks,
            mapYBlocks = request.mapYBlocks,
            deduper = deduper,
        )
        if (splitResult.status != RenderStatus.SUCCEED) {
            throw RenderPipelineException(splitResult.status)
        }
        return splitResult.tileIndexes!!
    }
}

/**
 * 仅用于本文件内部的“状态冒泡”异常。
 *
 * 目的：在逐帧渲染循环中，把底层阶段返回的可预期失败状态（例如 UNIQUE_TILE_OVERFLOW）
 * 直接抛回顶层统一转换为 RenderResult.failed(status)，避免多层样板式状态透传。
 *
 * 说明：
 * - 这不是通用异常类型，也不用于跨模块传播。
 * - 仅用于少见失败分支，正常热路径不会触发该异常。
 */
private class RenderPipelineException(val status: RenderStatus) : RuntimeException()
