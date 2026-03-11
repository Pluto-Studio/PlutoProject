package plutoproject.feature.gallery.core.render.mapcolor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.RgbaImage8888

class AlphaCompositorTest {
    @Test
    fun `alpha zero should be transparent and rgb set to zero`() {
        val source = RgbaImage8888(
            width = 1,
            height = 1,
            pixels = intArrayOf(rgba(0, 255, 0, 0)),
        )

        val composited = DefaultAlphaCompositor.composite(source, backgroundRgb24 = 0x112233)

        assertTrue(composited.transparentMask[0])
        assertEquals(0x000000, composited.rgb24Pixels[0])
    }

    @Test
    fun `alpha non-zero should composite with background`() {
        val source = RgbaImage8888(
            width = 1,
            height = 1,
            pixels = intArrayOf(rgba(128, 255, 0, 0)),
        )

        val composited = DefaultAlphaCompositor.composite(source, backgroundRgb24 = 0x0000FF)

        assertFalse(composited.transparentMask[0])
        val red = (composited.rgb24Pixels[0] ushr 16) and 0xFF
        val green = (composited.rgb24Pixels[0] ushr 8) and 0xFF
        val blue = composited.rgb24Pixels[0] and 0xFF
        assertEquals(128, red)
        assertEquals(0, green)
        assertEquals(127, blue)
    }

    @Test
    fun `alpha full should keep source rgb`() {
        val source = RgbaImage8888(
            width = 1,
            height = 1,
            pixels = intArrayOf(rgba(255, 12, 34, 56)),
        )

        val composited = DefaultAlphaCompositor.composite(source, backgroundRgb24 = 0xABCDEF)

        assertFalse(composited.transparentMask[0])
        assertEquals(0x0C2238, composited.rgb24Pixels[0])
    }
}

private fun rgba(a: Int, r: Int, g: Int, b: Int): Int {
    return ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
}
