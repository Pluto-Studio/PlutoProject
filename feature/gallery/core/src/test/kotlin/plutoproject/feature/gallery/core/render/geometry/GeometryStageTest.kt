package plutoproject.feature.gallery.core.render.geometry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.RgbaImage8888

class GeometryStageTest {
    @Test
    fun `cover should crop overflowing axis and keep center`() {
        val transform = CoverRepositioner.reposition(
            sourceWidth = 400,
            sourceHeight = 200,
            destinationWidth = 100,
            destinationHeight = 100,
        )

        assertEquals(100.0, transform.sourceStartX, 1e-9)
        assertEquals(0.0, transform.sourceStartY, 1e-9)
        assertEquals(200.0, transform.sourceSpanWidth, 1e-9)
        assertEquals(200.0, transform.sourceSpanHeight, 1e-9)
        assertEquals(100.5, transform.sourceXAt(0), 1e-9)
        assertEquals(298.5, transform.sourceXAt(99), 1e-9)
    }

    @Test
    fun `contain should introduce letterbox source range`() {
        val transform = ContainRepositioner.reposition(
            sourceWidth = 400,
            sourceHeight = 200,
            destinationWidth = 100,
            destinationHeight = 100,
        )

        assertEquals(0.0, transform.sourceStartX, 1e-9)
        assertEquals(-100.0, transform.sourceStartY, 1e-9)
        assertEquals(400.0, transform.sourceSpanWidth, 1e-9)
        assertEquals(400.0, transform.sourceSpanHeight, 1e-9)
        assertTrue(transform.sourceYAt(0) < 0.0)
    }

    @Test
    fun `scaler should preserve dimensions and identity sampling`() {
        val source = RgbaImage8888(
            width = 2,
            height = 2,
            pixels = intArrayOf(
                rgba(255, 255, 0, 0),
                rgba(255, 0, 255, 0),
                rgba(255, 0, 0, 255),
                rgba(255, 255, 255, 255),
            ),
        )

        val transform = StretchRepositioner.reposition(
            sourceWidth = source.width,
            sourceHeight = source.height,
            destinationWidth = source.width,
            destinationHeight = source.height,
        )

        val scaled = BilinearScaler.scale(source, transform)

        assertEquals(2, scaled.width)
        assertEquals(2, scaled.height)
        assertEquals(source.pixels.toList(), scaled.pixels.toList())
    }

    @Test
    fun `contain sampling should keep out-of-source area transparent`() {
        val source = solidImage(width = 4, height = 2, color = rgba(255, 200, 10, 10))
        val transform = ContainRepositioner.reposition(
            sourceWidth = source.width,
            sourceHeight = source.height,
            destinationWidth = 4,
            destinationHeight = 4,
        )

        val scaled = BilinearScaler.scale(source, transform)

        assertEquals(0, alpha(scaled.pixels[0]))
        assertTrue(alpha(scaled.pixels[4 + 1]) > 0)
    }

    @Test
    fun `extreme aspect ratio should not crash`() {
        val source = solidImage(width = 1, height = 4096, color = rgba(255, 20, 200, 20))
        val transform = CoverRepositioner.reposition(
            sourceWidth = source.width,
            sourceHeight = source.height,
            destinationWidth = 128,
            destinationHeight = 128,
        )

        val scaled = BilinearScaler.scale(source, transform)
        assertEquals(128, scaled.width)
        assertEquals(128, scaled.height)
    }

    @Test
    fun `target resolution should be map blocks times 128`() {
        val resolution = calcTargetResolution(mapXBlocks = 3, mapYBlocks = 2)

        assertEquals(384, resolution.width)
        assertEquals(256, resolution.height)
    }
}

private fun solidImage(width: Int, height: Int, color: Int): RgbaImage8888 {
    return RgbaImage8888(width, height, IntArray(width * height) { color })
}

private fun rgba(a: Int, r: Int, g: Int, b: Int): Int {
    return ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
}

private fun alpha(argb: Int): Int = (argb ushr 24) and 0xFF
