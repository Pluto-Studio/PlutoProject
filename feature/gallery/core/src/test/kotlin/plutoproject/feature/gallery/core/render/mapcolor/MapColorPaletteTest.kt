package plutoproject.feature.gallery.core.render.mapcolor

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class MapColorPaletteTest {
    @Test
    fun `should build vanilla palette with expected candidates and rgb lookup`() {
        val palette = MapColorPalette.vanilla()

        assertEquals(244, palette.candidates.size)
        palette.candidates.forEachIndexed { index, mapColor ->
            assertEquals(index + 4, mapColor.toUnsignedInt())
        }

        assertEquals(256, palette.rgbOfMapColor.size)
        assertEquals(0x000000, palette.rgbOfMapColor[0])
        assertEquals(0x000000, palette.rgbOfMapColor[1])
        assertEquals(0x000000, palette.rgbOfMapColor[2])
        assertEquals(0x000000, palette.rgbOfMapColor[3])

        assertEquals(0x597D27, palette.rgbOfMapColor[4])
        assertEquals(0x6D9930, palette.rgbOfMapColor[5])
        assertEquals(0x7FB238, palette.rgbOfMapColor[6])
        assertEquals(0x435E1D, palette.rgbOfMapColor[7])

        assertEquals(0xFFFFFF, palette.rgbOfMapColor[34])
        assertEquals(0x14B485, palette.rgbOfMapColor[234])
        assertEquals(0x43584F, palette.rgbOfMapColor[247])
    }

    @Test
    fun `should build rgb565 mapping table without transparent aliases`() {
        val table = calcRgb565ToMapColor(MapColorPalette.vanilla())

        table.forEachIndexed { rgb565, mapColor ->
            val mapColorInt = mapColor.toUnsignedInt()
            assertFalse(
                mapColorInt in 1..3,
                "rgb565=$rgb565 mapped to transparent alias $mapColorInt",
            )
        }
    }

    @Test
    fun `should build reproducible rgb565 mapping table`() {
        val palette = MapColorPalette.vanilla()

        val table1 = calcRgb565ToMapColor(palette)
        val table2 = calcRgb565ToMapColor(palette)

        assertArrayEquals(table1, table2)
    }

    @Test
    fun `should map sampled rgb colors to stable map colors`() {
        val table = calcRgb565ToMapColor(MapColorPalette.vanilla())

        val sampledExpectations = linkedMapOf(
            0x7FB238 to 6,
            0x597D27 to 4,
            0xFFFFFF to 34,
            0xFF0000 to 18,
            0x4040FF to 50,
            0x14B485 to 234,
            0x0000FF to 49,
            0x00FF00 to 134,
            0x111111 to 116,
            0x808080 to 89,
            0xD8AF93 to 242,
        )

        sampledExpectations.forEach { (rgb24, expectedMapColor) ->
            val rgb565 = rgb24ToRgb565(rgb24)
            val actualMapColor = table[rgb565].toUnsignedInt()

            assertEquals(
                expectedMapColor,
                actualMapColor,
                "rgb24=0x${rgb24.toString(16).uppercase()} rgb565=$rgb565",
            )
        }
    }
}

private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
