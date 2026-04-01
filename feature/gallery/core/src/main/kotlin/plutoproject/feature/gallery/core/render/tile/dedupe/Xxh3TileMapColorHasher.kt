package plutoproject.feature.gallery.core.render.tile.dedupe

import com.dynatrace.hash4j.hashing.Hashing
import plutoproject.feature.gallery.core.render.tile.TILE_PIXEL_COUNT

internal object Xxh3TileMapColorHasher : TileMapColorHasher {
    private val hasher = Hashing.xxh3_64()

    override fun hash(tileMapColors: ByteArray): Long {
        require(tileMapColors.size == TILE_PIXEL_COUNT) {
            "tileMapColors size must be ${TILE_PIXEL_COUNT}, actual=${tileMapColors.size}"
        }
        return hasher.hashBytesToLong(tileMapColors, 0, tileMapColors.size)
    }
}
