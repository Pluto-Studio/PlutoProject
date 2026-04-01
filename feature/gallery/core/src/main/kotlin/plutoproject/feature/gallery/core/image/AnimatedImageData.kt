package plutoproject.feature.gallery.core.image

import kotlin.time.Duration
import plutoproject.feature.gallery.core.render.tile.TilePool

@OptIn(ExperimentalUnsignedTypes::class)
class AnimatedImageData(
    val frameCount: Int,
    val duration: Duration,
    val tilePool: TilePool,
    val tileIndexes: UShortArray,
)
