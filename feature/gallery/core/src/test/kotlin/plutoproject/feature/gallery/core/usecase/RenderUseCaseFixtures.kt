package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.AnimatedImageData
import plutoproject.feature.gallery.core.StaticImageData
import plutoproject.feature.gallery.core.TilePool
import plutoproject.feature.gallery.core.decode.AnimatedFrameTiming
import plutoproject.feature.gallery.core.decode.DecodedAnimatedFrame
import plutoproject.feature.gallery.core.decode.DecodedAnimatedFrameStream
import plutoproject.feature.gallery.core.decode.DecodedAnimatedImageSource
import plutoproject.feature.gallery.core.render.RenderAnimatedImageRequest
import plutoproject.feature.gallery.core.render.RenderProfile
import plutoproject.feature.gallery.core.render.RenderStaticImageRequest
import plutoproject.feature.gallery.core.render.RgbaImage8888

internal fun sampleRgbaImage(): RgbaImage8888 = RgbaImage8888(
    width = 1,
    height = 1,
    pixels = intArrayOf(0xFFFFFFFF.toInt()),
)

internal fun staticRequest(
    mapXBlocks: Int = 1,
    mapYBlocks: Int = 1,
): RenderStaticImageRequest = RenderStaticImageRequest(
    sourceImage = sampleRgbaImage(),
    mapXBlocks = mapXBlocks,
    mapYBlocks = mapYBlocks,
    profile = RenderProfile(),
)

internal fun animatedRequest(
    sourceFrameCount: Int = 1,
    mapXBlocks: Int = 1,
    mapYBlocks: Int = 1,
): RenderAnimatedImageRequest = RenderAnimatedImageRequest(
    source = testAnimatedImageSource(
        List(sourceFrameCount) { index ->
            DecodedAnimatedFrame(
                sourceFrameIndex = index,
                delayCentiseconds = 1,
                image = sampleRgbaImage(),
            )
        }
    ),
    mapXBlocks = mapXBlocks,
    mapYBlocks = mapYBlocks,
    profile = RenderProfile(),
)

internal fun testAnimatedImageSource(
    frames: List<DecodedAnimatedFrame>,
): DecodedAnimatedImageSource {
    return object : DecodedAnimatedImageSource {
        override val width: Int = frames.firstOrNull()?.image?.width ?: 1
        override val height: Int = frames.firstOrNull()?.image?.height ?: 1
        override val frameCount: Int = frames.size
        override val frameTimeline: List<AnimatedFrameTiming> = frames.map {
            AnimatedFrameTiming(
                sourceFrameIndex = it.sourceFrameIndex,
                delayCentiseconds = it.delayCentiseconds,
            )
        }

        override suspend fun openFrameStream(): DecodedAnimatedFrameStream {
            return object : DecodedAnimatedFrameStream {
                private var cursor = 0

                override suspend fun nextFrame(): DecodedAnimatedFrame? {
                    if (cursor >= frames.size) {
                        return null
                    }
                    return frames[cursor++]
                }

                override fun close() = Unit
            }
        }
    }
}

internal fun decodedFrame(
    sourceFrameIndex: Int,
    delayCentiseconds: Int,
    image: RgbaImage8888,
): DecodedAnimatedFrame = DecodedAnimatedFrame(
    sourceFrameIndex = sourceFrameIndex,
    delayCentiseconds = delayCentiseconds,
    image = image,
)

internal fun tilePool(uniqueTileCount: Int): TilePool = TilePool(
    offsets = IntArray(uniqueTileCount + 1),
    blob = byteArrayOf(),
)

internal fun staticImageData(
    tileIndexesSize: Int,
    uniqueTileCount: Int = 1,
): StaticImageData = StaticImageData(
    tilePool = tilePool(uniqueTileCount),
    tileIndexes = ShortArray(tileIndexesSize),
)

internal fun animatedImageData(
    frameCount: Int,
    durationMillis: Int,
    tileIndexesSize: Int,
    uniqueTileCount: Int = 1,
): AnimatedImageData = AnimatedImageData(
    frameCount = frameCount,
    durationMillis = durationMillis,
    tilePool = tilePool(uniqueTileCount),
    tileIndexes = ShortArray(tileIndexesSize),
)
