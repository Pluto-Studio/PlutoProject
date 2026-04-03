package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class UnifiedImageDecoderTest {
    @Test
    fun `should wrap static decode success for webp`() = runTest {
        val result = UnifiedImageDecoder.decode(
            bytes = decodeBase64(WEBP_1X1_TRANSPARENT_BASE64),
            constraints = DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
        )

        assertTrue(result is UnifiedImageDecoder.Result.StaticSuccess)
        val decodeResult = (result as UnifiedImageDecoder.Result.StaticSuccess).result
        assertTrue(decodeResult is DecodeResult.Success)
        val image = (decodeResult as DecodeResult.Success).data
        assertEquals(1, image.width)
        assertEquals(1, image.height)
    }

    @Test
    fun `should wrap animated decode success for gif`() = runTest {
        val result = UnifiedImageDecoder.decode(
            bytes = decodeBase64(GIF_PATCH_TIMELINE_BASE64),
            constraints = DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
        )

        assertTrue(result is UnifiedImageDecoder.Result.AnimatedSuccess)
        val decodeResult = (result as UnifiedImageDecoder.Result.AnimatedSuccess).result
        assertTrue(decodeResult is DecodeResult.Success)
        val image = (decodeResult as DecodeResult.Success).data
        assertEquals(2, image.metadata.width)
        assertEquals(1, image.metadata.height)
        assertEquals(2, image.metadata.frameCount)
    }

    @Test
    fun `should return failure wrapping unsupported format when sniffer cannot identify image`() = runTest {
        val result = UnifiedImageDecoder.decode(
            bytes = encodePng(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)).drop(8).toByteArray(),
            constraints = DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
            fileNameHint = "unknown.bin",
        )

        assertEquals(
            UnifiedImageDecoder.Result.Failure(DecodeResult.UnsupportedFormat),
            result,
        )
    }
}

private fun encodePng(image: BufferedImage): ByteArray {
    val output = ByteArrayOutputStream()
    val written = ImageIO.write(image, "png", output)
    check(written) { "ImageIO cannot write format: png" }
    return output.toByteArray()
}
