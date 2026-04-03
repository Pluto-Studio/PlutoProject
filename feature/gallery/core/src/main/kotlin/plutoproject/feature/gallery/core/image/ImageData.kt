package plutoproject.feature.gallery.core.image

import plutoproject.feature.gallery.core.render.tile.TilePool
import kotlin.time.Duration

sealed interface ImageData {
    val type: ImageType
    val tilePool: TilePool
    val tileIndexes: ShortArray

    class Static(
        override val tilePool: TilePool,
        override val tileIndexes: ShortArray
    ) : ImageData {
        override val type: ImageType = ImageType.STATIC
    }

    class Animated(
        override val tilePool: TilePool,
        override val tileIndexes: ShortArray,
        val frameCount: Int,
        val duration: Duration,
    ) : ImageData {
        override val type: ImageType = ImageType.ANIMATED
    }
}
