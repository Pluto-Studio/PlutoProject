package plutoproject.feature.gallery.core.render.quantize

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MapColorPaletteTest {
    @Test
    fun `should expose stable vanilla rgb mapping for sampled map colors`() {
        assertEquals(0x000000, MapColorPalette.rgb24OfMapColor(0))
        assertEquals(0x597D27, MapColorPalette.rgb24OfMapColor(4))
        assertEquals(0x6D9930, MapColorPalette.rgb24OfMapColor(5))
        assertEquals(0x7FB238, MapColorPalette.rgb24OfMapColor(6))
        assertEquals(0x435E1D, MapColorPalette.rgb24OfMapColor(7))
        assertEquals(0xFFFFFF, MapColorPalette.rgb24OfMapColor(34))
        assertEquals(0x14B485, MapColorPalette.rgb24OfMapColor(234.toByte()))
        assertEquals(0x43584F, MapColorPalette.rgb24OfMapColor(247.toByte()))
    }

    @Test
    fun `nearest quantizer should map sampled colors to expected map colors`() {
        assertEquals(6, NearestColorQuantizer.quantize(0x7FB238).toUnsignedInt())
        assertEquals(4, NearestColorQuantizer.quantize(0x597D27).toUnsignedInt())
        assertEquals(34, NearestColorQuantizer.quantize(0xFFFFFF).toUnsignedInt())
        assertEquals(18, NearestColorQuantizer.quantize(0xFF0000).toUnsignedInt())
        assertEquals(50, NearestColorQuantizer.quantize(0x4040FF).toUnsignedInt())
        assertEquals(234, NearestColorQuantizer.quantize(0x14B485).toUnsignedInt())
    }
}

private fun MapColorPalette.rgb24OfMapColor(mapColor: Int): Int = rgb24OfMapColor(mapColor.toByte())
private fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
