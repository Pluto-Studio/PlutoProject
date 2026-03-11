package plutoproject.feature.gallery.core.render.mapcolor

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.DitherAlgorithm

class MapColorQuantizerTest {
    private val quantizer = newDefaultMapColorQuantizer()

    @Test
    fun `transparent pixels should always map to zero`() {
        val composited = CompositedRgbImage(
            width = 2,
            height = 1,
            rgb24Pixels = intArrayOf(0x123456, 0x654321),
            transparentMask = booleanArrayOf(true, true),
        )

        val none = quantizer.quantize(composited, DitherAlgorithm.NONE)
        val ordered = quantizer.quantize(composited, DitherAlgorithm.ORDERED_BAYER)
        val floyd = quantizer.quantize(composited, DitherAlgorithm.FLOYD_STEINBERG)

        assertArrayEquals(byteArrayOf(0, 0), none)
        assertArrayEquals(byteArrayOf(0, 0), ordered)
        assertArrayEquals(byteArrayOf(0, 0), floyd)
    }

    @Test
    fun `opaque pixels should never map to transparent aliases`() {
        val composited = CompositedRgbImage(
            width = 8,
            height = 8,
            rgb24Pixels = IntArray(64) { index ->
                val r = (index * 37) and 0xFF
                val g = (index * 53) and 0xFF
                val b = (index * 97) and 0xFF
                (r shl 16) or (g shl 8) or b
            },
            transparentMask = BooleanArray(64),
        )

        listOf(
            DitherAlgorithm.NONE,
            DitherAlgorithm.ORDERED_BAYER,
            DitherAlgorithm.FLOYD_STEINBERG,
        ).forEach { algorithm ->
            val mapColors = quantizer.quantize(composited, algorithm)
            mapColors.forEach { mapColor ->
                val mapColorInt = mapColor.toInt() and 0xFF
                assertFalse(mapColorInt in 0..3)
            }
        }
    }

    @Test
    fun `ordered and floyd should be deterministic`() {
        val composited = CompositedRgbImage(
            width = 16,
            height = 16,
            rgb24Pixels = IntArray(256) { index ->
                val x = index % 16
                val y = index / 16
                val r = (x * 16) and 0xFF
                val g = (y * 16) and 0xFF
                val b = ((x + y) * 8) and 0xFF
                (r shl 16) or (g shl 8) or b
            },
            transparentMask = BooleanArray(256),
        )

        val ordered1 = quantizer.quantize(composited, DitherAlgorithm.ORDERED_BAYER)
        val ordered2 = quantizer.quantize(composited, DitherAlgorithm.ORDERED_BAYER)
        val floyd1 = quantizer.quantize(composited, DitherAlgorithm.FLOYD_STEINBERG)
        val floyd2 = quantizer.quantize(composited, DitherAlgorithm.FLOYD_STEINBERG)

        assertArrayEquals(ordered1, ordered2)
        assertArrayEquals(floyd1, floyd2)
    }

    @Test
    fun `floyd should diffuse error across rows`() {
        val first = CompositedRgbImage(
            width = 1,
            height = 2,
            rgb24Pixels = intArrayOf(0x000000, 0x777777),
            transparentMask = booleanArrayOf(false, false),
        )
        val second = CompositedRgbImage(
            width = 1,
            height = 2,
            rgb24Pixels = intArrayOf(0xFFFFFF, 0x777777),
            transparentMask = booleanArrayOf(false, false),
        )

        val noneFirst = quantizer.quantize(first, DitherAlgorithm.NONE)
        val noneSecond = quantizer.quantize(second, DitherAlgorithm.NONE)
        val floydFirst = quantizer.quantize(first, DitherAlgorithm.FLOYD_STEINBERG)
        val floydSecond = quantizer.quantize(second, DitherAlgorithm.FLOYD_STEINBERG)

        assertTrue(noneFirst[1] == noneSecond[1])
        assertTrue(floydFirst[1] != floydSecond[1])
    }
}
