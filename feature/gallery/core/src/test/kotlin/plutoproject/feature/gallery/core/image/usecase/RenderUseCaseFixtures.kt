package plutoproject.feature.gallery.core.image.usecase

import plutoproject.feature.gallery.core.image.AnimatedImageData
import plutoproject.feature.gallery.core.image.StaticImageData
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
): StaticImageData = StaticImageData(
    tilePool = tilePool(uniqueTileCount),
    tileIndexes = UShortArray(tileIndexesSize),
)

internal fun animatedImageData(
    frameCount: Int,
    durationMillis: Int,
    tileIndexesSize: Int,
    uniqueTileCount: Int = 1,
): AnimatedImageData = AnimatedImageData(
    frameCount = frameCount,
    duration = durationMillis.milliseconds,
    tilePool = tilePool(uniqueTileCount),
    tileIndexes = UShortArray(tileIndexesSize),
)
