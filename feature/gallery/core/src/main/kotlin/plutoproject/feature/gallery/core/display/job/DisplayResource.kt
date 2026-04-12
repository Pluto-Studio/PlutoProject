package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.render.tile.TilePool
import plutoproject.feature.gallery.core.render.tile.codec.TileDecoder
import kotlin.time.Duration

class DecodedTilePool private constructor(
    private val decodedTiles: Array<ByteArray>,
) {
    val tileCount: Int
        get() = decodedTiles.size

    fun getTile(index: Int): ByteArray {
        require(index in decodedTiles.indices) { "index out of range: $index" }
        return decodedTiles[index]
    }

    companion object {
        fun from(tilePool: TilePool): DecodedTilePool {
            return DecodedTilePool(
                Array(tilePool.tileCount) { index ->
                    TileDecoder.decode(tilePool.getTile(index).toByteArray())
                }
            )
        }
    }
}

sealed interface DisplayResource {
    val type: ImageType
}

class StaticDisplayResource(
    val tileIndexes: ShortArray,
    val decodedTilePool: DecodedTilePool,
) : DisplayResource {
    override val type: ImageType = ImageType.STATIC
}

class AnimatedDisplayResource(
    val tileIndexes: ShortArray,
    val decodedTilePool: DecodedTilePool,
    val frameCount: Int,
    val duration: Duration,
) : DisplayResource {
    override val type: ImageType = ImageType.ANIMATED

    init {
        require(frameCount > 0) { "frameCount must be greater than 0" }
        require(duration.isPositive()) { "duration must be greater than 0" }
    }
}

class DisplayResourceFactory {
    fun create(data: ImageData): DisplayResource {
        val decodedTilePool = DecodedTilePool.from(data.tilePool)
        return when (data) {
            is ImageData.Static -> StaticDisplayResource(
                tileIndexes = data.tileIndexes,
                decodedTilePool = decodedTilePool,
            )

            is ImageData.Animated -> AnimatedDisplayResource(
                tileIndexes = data.tileIndexes,
                decodedTilePool = decodedTilePool,
                frameCount = data.frameCount,
                duration = data.duration,
            )
        }
    }
}
