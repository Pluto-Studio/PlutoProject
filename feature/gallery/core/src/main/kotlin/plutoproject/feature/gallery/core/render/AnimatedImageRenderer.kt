package plutoproject.feature.gallery.core.render

import plutoproject.feature.gallery.core.decode.animated.AnimatedImageSource
import plutoproject.feature.gallery.core.image.AnimatedImageData
import plutoproject.feature.gallery.core.render.framesample.FrameSamplingResult
import plutoproject.feature.gallery.core.render.tile.dedupe.TileDedupeResult
import plutoproject.feature.gallery.core.render.tile.dedupe.TileDeduper
import plutoproject.feature.gallery.core.render.tile.split.SplitTileGrid
import plutoproject.feature.gallery.core.util.checkpoint

@OptIn(ExperimentalUnsignedTypes::class)
object AnimatedImageRenderer {
    suspend fun render(
        source: AnimatedImageSource,
        settings: AnimatedImageRenderSettings
    ): RenderResult<AnimatedImageData> {
        val outputFrameTimeline = when (
            val samplingResult = settings.frameSampler.sample(
                sourceFrameTimeline = source.metadata.sourceFrameTimeline,
                minFrameDuration = settings.minFrameDuration,
                outputFrameInterval = settings.outputFrameInterval,
            )
        ) {
            is FrameSamplingResult.Success -> samplingResult.outputFrameTimeline
            FrameSamplingResult.DurationOverflow -> return RenderResult.DurationOverflow
            FrameSamplingResult.OutputFrameCountOverflow -> return RenderResult.OutputFrameCountOverflow
        }

        val singleFrameTileCount =
            settings.basicSettings.widthBlocks.toLong() * settings.basicSettings.heightBlocks.toLong()
        val totalTileIndexCount = singleFrameTileCount * outputFrameTimeline.frameCount.toLong()
        if (totalTileIndexCount > Int.MAX_VALUE.toLong()) {
            return RenderResult.TileIndexCountOverflow
        }

        val tileDeduper = TileDeduper()
        val requiredSourceFrameIndexes =
            collectRequiredSourceFrameIndexes(outputFrameTimeline.sourceFrameIndexByOutputFrame)
        val renderedFrameTileIndexes = HashMap<Int, UShortArray>(requiredSourceFrameIndexes.size)
        val stream = source.openFrameStream()

        try {
            while (true) {
                checkpoint()
                val frame = stream.nextFrame() ?: break
                if (frame.sourceFrameIndex !in requiredSourceFrameIndexes) {
                    continue
                }
                if (renderedFrameTileIndexes.containsKey(frame.sourceFrameIndex)) {
                    continue
                }

                val tileGrid = PixelBufferRenderer.render(frame.pixelBuffer, settings.basicSettings)
                val tileIndexes = dedupeTileGrid(tileGrid, tileDeduper) ?: return RenderResult.TilePoolOverflow
                renderedFrameTileIndexes[frame.sourceFrameIndex] = tileIndexes

                if (renderedFrameTileIndexes.size == requiredSourceFrameIndexes.size) {
                    break
                }
            }
        } finally {
            stream.close()
        }

        check(renderedFrameTileIndexes.keys.containsAll(requiredSourceFrameIndexes)) {
            "frame stream did not produce all required source frames"
        }

        val allFrameTileIndexes = UShortArray(totalTileIndexCount.toInt())
        var outputFrameIndex = 0
        while (outputFrameIndex < outputFrameTimeline.frameCount) {
            checkpoint()

            val sourceFrameIndex = outputFrameTimeline.sourceFrameIndexByOutputFrame[outputFrameIndex]
            val frameTileIndexes = checkNotNull(renderedFrameTileIndexes[sourceFrameIndex]) {
                "missing rendered tile indexes for source frame $sourceFrameIndex"
            }
            frameTileIndexes.copyInto(
                destination = allFrameTileIndexes,
                destinationOffset = outputFrameIndex * singleFrameTileCount.toInt(),
            )

            outputFrameIndex++
        }

        return RenderResult.Success(
            AnimatedImageData(
                frameCount = outputFrameTimeline.frameCount,
                duration = outputFrameTimeline.duration,
                tilePool = tileDeduper.buildTilePool(),
                tileIndexes = allFrameTileIndexes,
            )
        )
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
private suspend fun dedupeTileGrid(
    tileGrid: SplitTileGrid,
    tileDeduper: TileDeduper,
): UShortArray? {
    val tileIndexes = UShortArray(tileGrid.tileCount)

    for (tileIndex in 0 until tileGrid.tileCount) {
        checkpoint()
        when (val dedupeResult = tileDeduper.dedupe(tileGrid.getTile(tileIndex))) {
            is TileDedupeResult.Success -> tileIndexes[tileIndex] = dedupeResult.index.toUShort()
            TileDedupeResult.TilePoolOverflow -> return null
        }
    }

    return tileIndexes
}

private fun collectRequiredSourceFrameIndexes(sourceFrameIndexByOutputFrame: IntArray): Set<Int> {
    val requiredSourceFrameIndexes = HashSet<Int>(sourceFrameIndexByOutputFrame.size)

    for (sourceFrameIndex in sourceFrameIndexByOutputFrame) {
        requiredSourceFrameIndexes += sourceFrameIndex
    }

    return requiredSourceFrameIndexes
}
