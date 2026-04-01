package plutoproject.feature.gallery.core.render.alphacomposite

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.PixelBuffer
import plutoproject.feature.gallery.core.render.RenderWorkspace

class AlphaCompositorTest {
    @Test
    fun `alpha zero should be transparent and rgb set to zero`() {
        val workspace = RenderWorkspace(
            width = 1,
            height = 1,
            pixelBuffer = PixelBuffer(width = 1, height = 1, pixels = intArrayOf(rgba(0, 255, 0, 0))),
        )

        DefaultAlphaCompositor.composite(workspace, backgroundRgb24 = 0x112233)

        assertTrue(workspace.transparentMask!!.isTransparent(0))
        assertEquals(0x000000, workspace.pixelBuffer!!.pixels[0])
    }

    @Test
    fun `alpha non-zero should composite with background`() {
        val workspace = RenderWorkspace(
            width = 1,
            height = 1,
            pixelBuffer = PixelBuffer(width = 1, height = 1, pixels = intArrayOf(rgba(128, 255, 0, 0))),
        )

        DefaultAlphaCompositor.composite(workspace, backgroundRgb24 = 0x0000FF)
        val compositedPixel = workspace.pixelBuffer!!.pixels[0]

        assertFalse(workspace.transparentMask?.isTransparent(0) == true)
        val red = (compositedPixel ushr 16) and 0xFF
        val green = (compositedPixel ushr 8) and 0xFF
        val blue = compositedPixel and 0xFF
        assertEquals(128, red)
        assertEquals(0, green)
        assertEquals(127, blue)
    }

    @Test
    fun `alpha full should keep source rgb`() {
        val workspace = RenderWorkspace(
            width = 1,
            height = 1,
            pixelBuffer = PixelBuffer(width = 1, height = 1, pixels = intArrayOf(rgba(255, 12, 34, 56))),
        )

        DefaultAlphaCompositor.composite(workspace, backgroundRgb24 = 0xABCDEF)

        assertFalse(workspace.transparentMask?.isTransparent(0) == true)
        assertEquals(0x0C2238, workspace.pixelBuffer!!.pixels[0])
    }
}

private fun rgba(a: Int, r: Int, g: Int, b: Int): Int {
    return ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
}
