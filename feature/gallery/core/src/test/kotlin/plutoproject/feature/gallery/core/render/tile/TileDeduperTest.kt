package plutoproject.feature.gallery.core.render.tile

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.TilePool

class TileDeduperTest {
    @Test
    fun `should reuse tile index when tile content is identical`() {
        val deduper = TileDeduper()

        val firstTile = solidTile(7)
        val secondTile = solidTile(13)

        assertEquals(TileDedupeResult.Indexed(0), deduper.dedupe(firstTile))
        assertEquals(TileDedupeResult.Indexed(0), deduper.dedupe(firstTile.copyOf()))
        assertEquals(TileDedupeResult.Indexed(1), deduper.dedupe(secondTile))
        assertEquals(2, deduper.uniqueTileCount)

        val tilePool = deduper.buildTilePool()
        assertArrayEquals(firstTile, decodeTile(tileDataAt(tilePool, 0)))
        assertArrayEquals(secondTile, decodeTile(tileDataAt(tilePool, 1)))
    }

    @Test
    fun `should handle hash collision by tile-byte comparison`() {
        val deduper = TileDeduper(
            tileMapColorHasher = TileMapColorHasher { 0x1145141919810L },
        )

        val firstTile = solidTile(21)
        val secondTile = solidTile(22)

        assertEquals(TileDedupeResult.Indexed(0), deduper.dedupe(firstTile))
        assertEquals(TileDedupeResult.Indexed(1), deduper.dedupe(secondTile))
        assertEquals(TileDedupeResult.Indexed(0), deduper.dedupe(firstTile.copyOf()))
        assertEquals(2, deduper.uniqueTileCount)
    }

    @Test
    fun `should return overflow result when unique tile count exceeds limit`() {
        val deduper = TileDeduper(maxUniqueTileCount = 2)

        assertEquals(TileDedupeResult.Indexed(0), deduper.dedupe(solidTile(31)))
        assertEquals(TileDedupeResult.Indexed(1), deduper.dedupe(solidTile(32)))
        assertEquals(TileDedupeResult.UniqueTileOverflow, deduper.dedupe(solidTile(33)))

        val tilePool = deduper.buildTilePool()
        assertEquals(2, tilePool.offsets.size - 1)
    }
}

private fun solidTile(mapColor: Int): ByteArray {
    require(mapColor in 0..255) { "mapColor must be in [0, 255], actual=$mapColor" }
    return ByteArray(TILE_PIXEL_COUNT) { mapColor.toByte() }
}

private fun tileDataAt(tilePool: TilePool, tilePoolIndex: Int): ByteArray {
    require(tilePoolIndex in 0 until tilePool.offsets.size - 1) {
        "tilePoolIndex out of range: index=$tilePoolIndex, tileCount=${tilePool.offsets.size - 1}"
    }

    val start = tilePool.offsets[tilePoolIndex]
    val end = tilePool.offsets[tilePoolIndex + 1]
    return tilePool.blob.copyOfRange(start, end)
}
