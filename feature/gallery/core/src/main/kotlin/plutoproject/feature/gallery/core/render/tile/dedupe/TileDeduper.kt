package plutoproject.feature.gallery.core.render.tile.dedupe

import com.dynatrace.hash4j.hashing.Hashing
import plutoproject.feature.gallery.core.image.TilePool
import plutoproject.feature.gallery.core.render.tile.codec.TILE_PIXEL_COUNT
import plutoproject.feature.gallery.core.render.tile.codec.encodeTile

internal const val MAX_TILE_POOL_UNIQUE_TILE_COUNT = 65_536

internal sealed interface TileDedupeResult {
    data class Indexed(val tilePoolIndex: Int) : TileDedupeResult

    data object UniqueTileOverflow : TileDedupeResult
}

internal fun interface TileMapColorHasher {
    fun hash(tileMapColors: ByteArray): Long
}

internal object Xxh3TileMapColorHasher : TileMapColorHasher {
    private val hasher = Hashing.xxh3_64()

    override fun hash(tileMapColors: ByteArray): Long {
        require(tileMapColors.size == TILE_PIXEL_COUNT) {
            "tileMapColors size must be ${TILE_PIXEL_COUNT}, actual=${tileMapColors.size}"
        }
        return hasher.hashBytesToLong(tileMapColors, 0, tileMapColors.size)
    }
}

/**
 * 基于 tile mapColor 内容去重，产出 TilePool index。
 */
internal class TileDeduper(
    private val tilePoolBuilder: TilePoolBuilder = TilePoolBuilder(),
    private val tileMapColorHasher: TileMapColorHasher = Xxh3TileMapColorHasher,
    private val maxUniqueTileCount: Int = MAX_TILE_POOL_UNIQUE_TILE_COUNT,
) {
    private val bucketByHash = HashMap<Long, IntIndexBucket>()

    init {
        require(maxUniqueTileCount in 1..MAX_TILE_POOL_UNIQUE_TILE_COUNT) {
            "maxUniqueTileCount must be in [1, $MAX_TILE_POOL_UNIQUE_TILE_COUNT], actual=$maxUniqueTileCount"
        }
    }

    val uniqueTileCount: Int
        get() = tilePoolBuilder.uniqueTileCount

    fun dedupe(tileMapColors: ByteArray): TileDedupeResult {
        require(tileMapColors.size == TILE_PIXEL_COUNT) {
            "tileMapColors size must be ${TILE_PIXEL_COUNT}, actual=${tileMapColors.size}"
        }

        val hash = tileMapColorHasher.hash(tileMapColors)
        val tileData = encodeTile(tileMapColors)

        val bucket = bucketByHash[hash]
        val reusedIndex = bucket?.findFirst { candidateIndex ->
            tilePoolBuilder.tileDataEquals(candidateIndex, tileData)
        }
        if (reusedIndex != null) {
            return TileDedupeResult.Indexed(reusedIndex)
        }

        if (tilePoolBuilder.uniqueTileCount >= maxUniqueTileCount) {
            return TileDedupeResult.UniqueTileOverflow
        }

        val tilePoolIndex = tilePoolBuilder.appendTile(tileData)
        if (bucket == null) {
            bucketByHash[hash] = IntIndexBucket().also { it.add(tilePoolIndex) }
        } else {
            bucket.add(tilePoolIndex)
        }
        return TileDedupeResult.Indexed(tilePoolIndex)
    }

    fun buildTilePool(): TilePool = tilePoolBuilder.build()
}
