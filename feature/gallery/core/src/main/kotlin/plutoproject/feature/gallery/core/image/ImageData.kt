package plutoproject.feature.gallery.core.image

import plutoproject.feature.gallery.core.render.tile.TilePool
import kotlin.time.Duration

sealed interface ImageData {
    val type: ImageType
    val tilePool: TilePool
    val tileIndexes: ShortArray

    fun isStatic(): Boolean = this is Static

    fun isAnimated(): Boolean = this is Animated

    fun asStaticOrNull(): Static? = this as? Static

    fun asAnimatedOrNull(): Animated? = this as? Animated

    fun asStatic(): Static = asStaticOrNull() ?: error("Type mismatch, expected static")

    fun asAnimated(): Animated = asAnimatedOrNull() ?: error("Type mismatch, expected animated")

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
