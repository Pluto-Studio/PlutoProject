package plutoproject.feature.gallery.core.render.tile.dedupe

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.tile.TILE_PIXEL_COUNT
import plutoproject.feature.gallery.core.render.tile.TilePixelsView
import plutoproject.feature.gallery.core.render.tile.codec.TileDecoder

class TileDeduperTest {
    @Test
    fun `should reuse tile index when tile content is identical`() {
        val deduper = TileDeduper()

        val firstTile = solidTile(7)
        val secondTile = solidTile(13)

        assertEquals(TileDedupeResult.Success(0), deduper.dedupe(tileView(firstTile)))
        assertEquals(TileDedupeResult.Success(0), deduper.dedupe(tileView(firstTile.copyOf())))
        assertEquals(TileDedupeResult.Success(1), deduper.dedupe(tileView(secondTile)))
        assertEquals(2, deduper.uniqueTileCount)

        val tilePool = deduper.buildTilePool()
        assertArrayEquals(firstTile, TileDecoder.decode(tilePool.getTile(0).toByteArray()))
        assertArrayEquals(secondTile, TileDecoder.decode(tilePool.getTile(1).toByteArray()))
    }

    @Test
    fun `should handle hash collision by tile-byte comparison`() {
        val deduper = TileDeduper(
            tileMapColorHasher = TileMapColorHasher { 0x1145141919810L },
        )

        val firstTile = solidTile(21)
        val secondTile = solidTile(22)

        assertEquals(TileDedupeResult.Success(0), deduper.dedupe(tileView(firstTile)))
        assertEquals(TileDedupeResult.Success(1), deduper.dedupe(tileView(secondTile)))
        assertEquals(TileDedupeResult.Success(0), deduper.dedupe(tileView(firstTile.copyOf())))
        assertEquals(2, deduper.uniqueTileCount)
    }

}

private fun tileView(tile: ByteArray): TilePixelsView {
    require(tile.size == TILE_PIXEL_COUNT)
    return TilePixelsView(tile, sourceWidth = 128, tileX = 0, tileY = 0)
}

private fun solidTile(mapColor: Int): ByteArray {
    require(mapColor in 0..255) { "mapColor must be in [0, 255], actual=$mapColor" }
    return ByteArray(TILE_PIXEL_COUNT) { mapColor.toByte() }
}
