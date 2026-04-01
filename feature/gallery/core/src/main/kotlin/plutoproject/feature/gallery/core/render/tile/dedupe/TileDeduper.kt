package plutoproject.feature.gallery.core.render.tile.dedupe

import plutoproject.feature.gallery.core.render.tile.MAX_TILE_POOL_UNIQUE_TILE_COUNT
import plutoproject.feature.gallery.core.render.tile.MutableTilePool
import plutoproject.feature.gallery.core.render.tile.TILE_PIXEL_COUNT
import plutoproject.feature.gallery.core.render.tile.codec.TileEncoder
import plutoproject.feature.gallery.core.render.tile.TilePixelsView
import plutoproject.feature.gallery.core.render.tile.TilePool

internal class TileDeduper(
    private val tilePool: MutableTilePool = MutableTilePool(),
    private val tileMapColorHasher: TileMapColorHasher = Xxh3TileMapColorHasher,
) {
    private val tileIndexesByHash = HashMap<Long, IntIndexBucket>()
    private val tileScratchBuffer = ByteArray(TILE_PIXEL_COUNT)

    val uniqueTileCount: Int
        get() = tilePool.size

    fun dedupe(tile: TilePixelsView): TileDedupeResult {
        tile.copyTo(tileScratchBuffer)

        val hash = tileMapColorHasher.hash(tileScratchBuffer)
        val encodedTile = TileEncoder.encode(tileScratchBuffer)

        val bucket = tileIndexesByHash[hash]
        val reusedIndex = bucket?.findFirst { candidateIndex ->
            tilePool.encodedTileEquals(candidateIndex, encodedTile)
        }
        if (reusedIndex != null) {
            return TileDedupeResult.Success(reusedIndex)
        }

        if (tilePool.size >= MAX_TILE_POOL_UNIQUE_TILE_COUNT) {
            return TileDedupeResult.TilePoolOverflow
        }

        val tilePoolIndex = tilePool.addTile(encodedTile)
        if (bucket == null) {
            tileIndexesByHash[hash] = IntIndexBucket().also { it.add(tilePoolIndex) }
        } else {
            bucket.add(tilePoolIndex)
        }

        return TileDedupeResult.Success(tilePoolIndex)
    }

    fun buildTilePool(): TilePool = tilePool.freeze()
}

private class IntIndexBucket {
    private var values = IntArray(4)
    private var size = 0

    fun add(value: Int) {
        if (size == values.size) {
            values = values.copyOf(values.size shl 1)
        }
        values[size++] = value
    }

    fun findFirst(predicate: (Int) -> Boolean): Int? {
        var index = 0
        while (index < size) {
            val value = values[index]
            if (predicate(value)) {
                return value
            }
            index++
        }
        return null
    }
}
