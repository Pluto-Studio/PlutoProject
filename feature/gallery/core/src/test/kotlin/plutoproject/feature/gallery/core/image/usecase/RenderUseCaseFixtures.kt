package plutoproject.feature.gallery.core.image.usecase

import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.render.tile.TilePool
import plutoproject.feature.gallery.core.render.tile.TilePoolSnapshot
import kotlin.time.Duration.Companion.milliseconds

internal fun tilePool(uniqueTileCount: Int): TilePool = TilePool.fromSnapshot(
    TilePoolSnapshot(
        offsets = IntArray(uniqueTileCount + 1),
        blob = byteArrayOf(),
    )
)

internal fun staticImageData(
    tileIndexesSize: Int,
    uniqueTileCount: Int = 1,
): ImageData.Static = ImageData.Static(
    tilePool = tilePool(uniqueTileCount),
    tileIndexes = ShortArray(tileIndexesSize),
)

internal fun animatedImageData(
    frameCount: Int,
    durationMillis: Int,
    tileIndexesSize: Int,
    uniqueTileCount: Int = 1,
): ImageData.Animated = ImageData.Animated(
    tilePool = tilePool(uniqueTileCount),
    tileIndexes = ShortArray(tileIndexesSize),
    frameCount = frameCount,
    duration = durationMillis.milliseconds,
)
