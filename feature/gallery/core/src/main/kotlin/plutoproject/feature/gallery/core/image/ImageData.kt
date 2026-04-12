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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Static) return false
            if (!tilePool.contentEquals(other.tilePool)) return false
            if (!tileIndexes.contentEquals(other.tileIndexes)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = tilePool.contentHashCode()
            result = 31 * result + tileIndexes.contentHashCode()
            return result
        }
    }

    class Animated(
        override val tilePool: TilePool,
        override val tileIndexes: ShortArray,
        val frameCount: Int,
        val duration: Duration,
    ) : ImageData {
        override val type: ImageType = ImageType.ANIMATED

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Animated) return false
            if (frameCount != other.frameCount) return false
            if (duration != other.duration) return false
            if (!tilePool.contentEquals(other.tilePool)) return false
            if (!tileIndexes.contentEquals(other.tileIndexes)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = tilePool.contentHashCode()
            result = 31 * result + tileIndexes.contentHashCode()
            result = 31 * result + frameCount
            result = 31 * result + duration.hashCode()
            return result
        }
    }
}

private fun TilePool.contentEquals(other: TilePool): Boolean {
    val snapshot = snapshot()
    val otherSnapshot = other.snapshot()
    return snapshot.offsets.contentEquals(otherSnapshot.offsets)
        && snapshot.blob.contentEquals(otherSnapshot.blob)
}

private fun TilePool.contentHashCode(): Int {
    val snapshot = snapshot()
    var result = snapshot.offsets.contentHashCode()
    result = 31 * result + snapshot.blob.contentHashCode()
    return result
}
