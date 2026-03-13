package plutoproject.feature.gallery.core.usecase

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.DefaultStaticImageRenderer
import plutoproject.feature.gallery.core.render.DitherAlgorithm
import plutoproject.feature.gallery.core.render.RenderProfile
import plutoproject.feature.gallery.core.render.RenderResult
import plutoproject.feature.gallery.core.render.RenderStaticImageRequest
import plutoproject.feature.gallery.core.render.RepositionMode
import plutoproject.feature.gallery.core.render.RgbaImage8888
import plutoproject.feature.gallery.core.render.mapcolor.defaultAlphaCompositor
import plutoproject.feature.gallery.core.render.mapcolor.defaultMapColorQuantizer
import java.util.logging.Logger

class RenderStaticPipelineIntegrationTest {
    private val renderer = DefaultStaticImageRenderer(
        alphaCompositor = defaultAlphaCompositor(),
        mapColorQuantizer = defaultMapColorQuantizer(),
        logger = Logger.getLogger(DefaultStaticImageRenderer::class.java.name),
    )

    @Test
    fun `should keep tile indexes in left-right top-bottom order`() = runTest {
        val source = mutableImage(width = 256, height = 256)
        fillRect(source, x = 0, y = 0, width = 128, height = 128, argb = argb(255, 127, 178, 56))
        fillRect(source, x = 128, y = 0, width = 128, height = 128, argb = argb(255, 255, 0, 0))
        fillRect(source, x = 0, y = 128, width = 128, height = 128, argb = argb(255, 255, 255, 255))
        fillRect(source, x = 128, y = 128, width = 128, height = 128, argb = argb(255, 64, 64, 255))

        val useCase = RenderStaticImageUseCase(renderer)
        val result = useCase.execute(
            RenderStaticImageRequest(
                sourceImage = source,
                mapXBlocks = 2,
                mapYBlocks = 2,
                profile = defaultNoDitherProfile(),
            ),
        )

        assertTrue(result is RenderResult.Success)
        val imageData = (result as RenderResult.Success).imageData!!
        assertEquals(4, imageData.tileIndexes.size)
        assertEquals(listOf(0, 1, 2, 3), imageData.tileIndexes.map { it.toU16Int() })
    }

    @Test
    fun `should dedupe identical tiles across large static image`() = runTest {
        val source = mutableImage(width = 512, height = 512)
        fillRect(source, x = 0, y = 0, width = 512, height = 512, argb = argb(255, 127, 178, 56))

        val useCase = RenderStaticImageUseCase(renderer)
        val result = useCase.execute(
            RenderStaticImageRequest(
                sourceImage = source,
                mapXBlocks = 4,
                mapYBlocks = 4,
                profile = defaultNoDitherProfile(),
            ),
        )

        assertTrue(result is RenderResult.Success)
        val imageData = (result as RenderResult.Success).imageData!!
        assertEquals(16, imageData.tileIndexes.size)
        assertEquals(1, imageData.tilePool.offsets.size - 1)
        assertTrue(imageData.tileIndexes.all { it.toU16Int() == 0 })
    }

    @Test
    fun `should render expected tile indexes length for map size`() = runTest {
        val source = mutableImage(width = 384, height = 256)
        fillRect(source, x = 0, y = 0, width = 384, height = 256, argb = argb(255, 30, 200, 120))

        val useCase = RenderStaticImageUseCase(renderer)
        val result = useCase.execute(
            RenderStaticImageRequest(
                sourceImage = source,
                mapXBlocks = 3,
                mapYBlocks = 2,
                profile = defaultNoDitherProfile(),
            ),
        )

        assertTrue(result is RenderResult.Success)
        assertEquals(6, (result as RenderResult.Success).imageData!!.tileIndexes.size)
    }
}

private fun defaultNoDitherProfile(): RenderProfile = RenderProfile(
    repositionMode = RepositionMode.STRETCH,
    ditherAlgorithm = DitherAlgorithm.NONE,
)

private fun mutableImage(width: Int, height: Int): RgbaImage8888 {
    return RgbaImage8888(width, height, IntArray(width * height))
}

private fun fillRect(source: RgbaImage8888, x: Int, y: Int, width: Int, height: Int, argb: Int) {
    var dy = y
    while (dy < y + height) {
        var dx = x
        while (dx < x + width) {
            source.pixels[dy * source.width + dx] = argb
            dx++
        }
        dy++
    }
}

private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
    return ((alpha and 0xFF) shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)
}

private fun Short.toU16Int(): Int = toInt() and 0xFFFF
