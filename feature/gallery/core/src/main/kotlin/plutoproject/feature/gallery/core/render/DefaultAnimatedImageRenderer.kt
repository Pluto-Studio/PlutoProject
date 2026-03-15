package plutoproject.feature.gallery.core.render

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import plutoproject.feature.gallery.core.AnimatedImageData
import plutoproject.feature.gallery.core.decode.DecodedAnimatedFrame
import plutoproject.feature.gallery.core.render.geometry.TargetResolution
import plutoproject.feature.gallery.core.render.geometry.calcTargetResolution
import plutoproject.feature.gallery.core.render.geometry.repositionerOf
import plutoproject.feature.gallery.core.render.geometry.scalerOf
import plutoproject.feature.gallery.core.render.mapcolor.AlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.MapColorQuantizer
import plutoproject.feature.gallery.core.render.tile.TileDeduper
import plutoproject.feature.gallery.core.render.tile.TileSplitter
import java.util.logging.Level
import java.util.logging.Logger

class DefaultAnimatedImageRenderer(
    private val frameSampler: FrameSampler,
    private val alphaCompositor: AlphaCompositor,
    private val mapColorQuantizer: MapColorQuantizer,
    private val logger: Logger,
) : AnimatedImageRenderer {
    override suspend fun render(request: RenderAnimatedImageRequest): RenderResult<AnimatedImageData> = try {
        checkpoint()

        val frameSampleResult = frameSampler.sample(request.source.frameTimeline, request.profile)
        checkpoint()

        val (outToSourceFrameIndex, durationMillis) = when (frameSampleResult) {
            is FrameSampleResult.Failure -> return RenderResult.Failure(frameSampleResult.status)
            is FrameSampleResult.Success -> frameSampleResult.outToSourceFrameIndex to frameSampleResult.durationMillis
        }
        val targetResolution = calcTargetResolution(request.mapXBlocks, request.mapYBlocks)
        checkpoint()

        val singleFrameTileCount = request.mapXBlocks * request.mapYBlocks
        val totalTileIndexesLengthLong = singleFrameTileCount.toLong() * outToSourceFrameIndex.size.toLong()
        if (totalTileIndexesLengthLong > Int.MAX_VALUE.toLong()) {
            return RenderResult.failed(RenderStatus.TILE_INDEXES_LENGTH_OVERFLOW)
        }

        val allFrameTileIndexes = ShortArray(totalTileIndexesLengthLong.toInt())
        val deduper = TileDeduper()
        val renderedFrameCache = HashMap<Int, ShortArray>()
        val requiredSourceFrameIndexes = collectRequiredSourceFrameIndexes(
            outToSourceFrameIndex = outToSourceFrameIndex,
            sourceFrameCount = request.source.frameCount,
        ) ?: return RenderResult.failed(RenderStatus.INVALID_SOURCE_FRAME_COUNT)

        renderRequiredFrames(
            request = request,
            targetResolution = targetResolution,
            deduper = deduper,
            requiredSourceFrameIndexes = requiredSourceFrameIndexes,
            renderedFrameCache = renderedFrameCache,
        )

        if (!renderedFrameCache.keys.containsAll(requiredSourceFrameIndexes)) {
            return RenderResult.failed(RenderStatus.INVALID_SOURCE_FRAME_COUNT)
        }

        var outFrameIndex = 0
        checkpoint()

        while (outFrameIndex < outToSourceFrameIndex.size) {
            checkpoint()
            val srcFrameIndex = outToSourceFrameIndex[outFrameIndex]
            val frameTileIndexes = renderedFrameCache[srcFrameIndex]
                ?: return RenderResult.failed(RenderStatus.INVALID_SOURCE_FRAME_COUNT)

            frameTileIndexes.copyInto(
                destination = allFrameTileIndexes,
                destinationOffset = outFrameIndex * singleFrameTileCount,
            )
            outFrameIndex++
        }
        checkpoint()

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
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.log(
            Level.SEVERE,
            "Animated image render pipeline failed with internal error: sourceFrameCount=${request.source.frameCount}, mapXBlocks=${request.mapXBlocks}, mapYBlocks=${request.mapYBlocks}",
            e,
        )
        RenderResult.failed(RenderStatus.PIPELINE_FAILED)
    }

    private suspend fun renderRequiredFrames(
        request: RenderAnimatedImageRequest,
        targetResolution: TargetResolution,
        deduper: TileDeduper,
        requiredSourceFrameIndexes: Set<Int>,
        renderedFrameCache: MutableMap<Int, ShortArray>,
    ) {
        val stream = request.source.openFrameStream()
        try {
            while (true) {
                checkpoint()
                val frame = stream.nextFrame() ?: break
                val sourceFrameIndex = frame.sourceFrameIndex
                if (sourceFrameIndex !in requiredSourceFrameIndexes) {
                    continue
                }
                if (renderedFrameCache.containsKey(sourceFrameIndex)) {
                    continue
                }

                renderedFrameCache[sourceFrameIndex] = renderSourceFrameTileIndexes(
                    request = request,
                    sourceFrame = frame,
                    targetResolution = targetResolution,
                    deduper = deduper,
                )

                if (renderedFrameCache.size == requiredSourceFrameIndexes.size) {
                    return
                }
            }
        } finally {
            stream.close()
        }
    }

    private suspend fun renderSourceFrameTileIndexes(
        request: RenderAnimatedImageRequest,
        sourceFrame: DecodedAnimatedFrame,
        targetResolution: TargetResolution,
        deduper: TileDeduper,
    ): ShortArray {
        checkpoint()

        val sourceImage = sourceFrame.image
        checkpoint()

        val transform = repositionerOf(request.profile.repositionMode).reposition(
            sourceWidth = sourceImage.width,
            sourceHeight = sourceImage.height,
            destinationWidth = targetResolution.width,
            destinationHeight = targetResolution.height,
        )
        checkpoint()

        val scaledImage = scalerOf(request.profile.scaleAlgorithm).scale(sourceImage, transform)
        checkpoint()

        val composited = alphaCompositor.composite(scaledImage, request.profile.alphaBackgroundColorRgb)
        checkpoint()

        val mapColorPixels = mapColorQuantizer.quantize(composited, request.profile.ditherAlgorithm)
        checkpoint()

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
            throw RenderPipelineException(splitResult.status)
        }

        return splitResult.tileIndexes!!
    }
}

private fun collectRequiredSourceFrameIndexes(
    outToSourceFrameIndex: IntArray,
    sourceFrameCount: Int,
): Set<Int>? {
    val required = HashSet<Int>(outToSourceFrameIndex.size)
    for (index in outToSourceFrameIndex) {
        if (index !in 0 until sourceFrameCount) {
            return null
        }
        required += index
    }
    return required
}

private suspend fun checkpoint() {
    currentCoroutineContext().ensureActive()
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
