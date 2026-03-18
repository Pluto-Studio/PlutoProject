package plutoproject.feature.gallery.core.render.tile

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.tile.dedupe.TilePoolBuilder

class TilePoolBuilderTest {
    @Test
    fun `should append tile bytes and build tile pool with sentinel offsets`() {
        val builder = TilePoolBuilder(initialTileCapacity = 1, initialBlobCapacityBytes = 1)

        val first = byteArrayOf(1, 2, 3)
        val second = byteArrayOf(4, 5)

        assertEquals(0, builder.appendTile(first))
        assertEquals(1, builder.appendTile(second))

        val tilePool = builder.build()

        assertArrayEquals(intArrayOf(0, 3, 5), tilePool.offsets)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), tilePool.blob)
    }

    @Test
    fun `should compare tile bytes in blob without decode`() {
        val builder = TilePoolBuilder()
        val tileIndex = builder.appendTile(byteArrayOf(10, 11, 12))

        assertTrue(builder.tileDataEquals(tileIndex, byteArrayOf(10, 11, 12)))
        assertFalse(builder.tileDataEquals(tileIndex, byteArrayOf(10, 11)))
        assertFalse(builder.tileDataEquals(tileIndex, byteArrayOf(10, 11, 13)))
    }
}
