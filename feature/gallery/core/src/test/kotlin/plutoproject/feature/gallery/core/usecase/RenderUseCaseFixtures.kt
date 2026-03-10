package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.AnimatedImageData
import plutoproject.feature.gallery.core.StaticImageData
import plutoproject.feature.gallery.core.TilePool
import plutoproject.feature.gallery.core.render.AnimatedSourceFrame
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
    sourceFrames = List(sourceFrameCount) {
        AnimatedSourceFrame(
            image = sampleRgbaImage(),
            delayCentiseconds = 1,
        )
    },
    mapXBlocks = mapXBlocks,
    mapYBlocks = mapYBlocks,
    profile = RenderProfile(),
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
