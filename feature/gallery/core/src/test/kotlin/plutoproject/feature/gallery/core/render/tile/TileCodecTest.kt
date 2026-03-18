package plutoproject.feature.gallery.core.render.tile

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.tile.codec.BitReader
import plutoproject.feature.gallery.core.render.tile.codec.MAX_PALETTE_SIZE
import plutoproject.feature.gallery.core.render.tile.codec.TILE_PIXEL_COUNT
import plutoproject.feature.gallery.core.render.tile.codec.bitsPerPixel
import plutoproject.feature.gallery.core.render.tile.codec.decodePaletteSize
import plutoproject.feature.gallery.core.render.tile.codec.decodeTile
import plutoproject.feature.gallery.core.render.tile.codec.encodeTile

class TileCodecTest {
    @Test
    fun `should encode and decode solid tile with bpp0 and empty segments`() {
        val tile = ByteArray(TILE_PIXEL_COUNT) { 0x2A }

        val encoded = encodeTile(tile)
        val header = parseHeader(encoded)

        assertEquals(1, header.rawPaletteSize)
        assertEquals(1, header.paletteSize)
        assertEquals(0, header.segmentBytes)

        val decoded = decodeTile(encoded)
        assertArrayEquals(tile, decoded)
    }

    @Test
    fun `should encode decode correctly for palette sizes 2 16 255 and 256`() {
        val cases = listOf(
            2 to 2,
            16 to 16,
            255 to 255,
            256 to 0,
        )

        for ((uniqueColors, expectedRawPaletteSize) in cases) {
            val tile = buildPaletteCycleTile(uniqueColors)
            val encoded = encodeTile(tile)
            val header = parseHeader(encoded)

            assertEquals(
                expectedRawPaletteSize,
                header.rawPaletteSize,
                "uniqueColors=$uniqueColors",
            )
            assertEquals(uniqueColors, header.paletteSize, "uniqueColors=$uniqueColors")

            val decoded = decodeTile(encoded)
            assertArrayEquals(tile, decoded, "uniqueColors=$uniqueColors")
        }
    }

    @Test
    fun `should encode segments with both run and literal modes and decode byte-identical`() {
        val tile = ByteArray(TILE_PIXEL_COUNT)
        var offset = 0

        repeat(150) { tile[offset++] = 42 }
        repeat(64) { tile[offset++] = it.toByte() }
        repeat(80) { tile[offset++] = 11 }
        while (offset < TILE_PIXEL_COUNT) {
            tile[offset] = if ((offset and 1) == 0) 2 else 3
            offset++
        }

        val encoded = encodeTile(tile)
        val modeFlags = parseSegmentModes(encoded)

        assertTrue(modeFlags.hasRun)
        assertTrue(modeFlags.hasLiteral)

        val decoded = decodeTile(encoded)
        assertArrayEquals(tile, decoded)
    }
}

private data class TileHeader(
    val rawPaletteSize: Int,
    val paletteSize: Int,
    val segmentBytes: Int,
    val segmentsOffset: Int,
)

private data class SegmentModeFlags(
    val hasRun: Boolean,
    val hasLiteral: Boolean,
)

private fun parseHeader(tileData: ByteArray): TileHeader {
    require(tileData.isNotEmpty())

    val rawPaletteSize = tileData[0].toInt() and 0xFF
    val paletteSize = decodePaletteSize(rawPaletteSize)
    val segmentBytesOffset = 1 + paletteSize
    require(tileData.size >= segmentBytesOffset + 2)

    val segmentBytes =
        (tileData[segmentBytesOffset].toInt() and 0xFF) or
            ((tileData[segmentBytesOffset + 1].toInt() and 0xFF) shl 8)

    return TileHeader(
        rawPaletteSize = rawPaletteSize,
        paletteSize = paletteSize,
        segmentBytes = segmentBytes,
        segmentsOffset = segmentBytesOffset + 2,
    )
}

private fun parseSegmentModes(tileData: ByteArray): SegmentModeFlags {
    val header = parseHeader(tileData)
    val bpp = bitsPerPixel(header.paletteSize)
    if (header.segmentBytes == 0) {
        return SegmentModeFlags(hasRun = false, hasLiteral = false)
    }

    val reader = BitReader(tileData, startOffset = header.segmentsOffset, length = header.segmentBytes)
    var decodedPixels = 0
    var hasRun = false
    var hasLiteral = false

    while (decodedPixels < TILE_PIXEL_COUNT) {
        val control = reader.readBits(8)
        val isRun = (control and 0x80) != 0
        val length = (control and 0x7F) + 1

        if (isRun) {
            hasRun = true
            reader.readBits(bpp)
        } else {
            hasLiteral = true
            repeat(length) {
                reader.readBits(bpp)
            }
        }

        decodedPixels += length
    }

    return SegmentModeFlags(hasRun = hasRun, hasLiteral = hasLiteral)
}

private fun buildPaletteCycleTile(uniqueColors: Int): ByteArray {
    require(uniqueColors in 1..MAX_PALETTE_SIZE)

    return ByteArray(TILE_PIXEL_COUNT) { pixel ->
        (pixel % uniqueColors).toByte()
    }
}
