package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WebpDecoderTest {
    @Test
    fun `static image decoder should decode embedded webp sample`() = runTest {
        val result = withTempImageFile(decodeBase64(WEBP_1X1_TRANSPARENT_BASE64)) { path ->
            StaticImageDecoder.decode(
                path = path,
                constraints = DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
                inputStreamSpi = SupportedImageFormat.Webp.inputStreamSpi,
                readerSpi = SupportedImageFormat.Webp.readerSpi,
            )
        }

        assertTrue(result is DecodeResult.Success)
        val image = (result as DecodeResult.Success).data
        assertEquals(1, image.width)
        assertEquals(1, image.height)
        assertEquals(0, image.pixels[0] ushr 24)
    }

    @Test
    fun `static image decoder should return invalid-image for malformed webp bytes`() = runTest {
        val result = withTempImageFile(
            byteArrayOf(
                'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
                0, 0, 0, 0,
                'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte(),
            )
        ) { path ->
            StaticImageDecoder.decode(
                path = path,
                constraints = DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
                inputStreamSpi = SupportedImageFormat.Webp.inputStreamSpi,
                readerSpi = SupportedImageFormat.Webp.readerSpi,
            )
        }

        assertEquals(DecodeResult.InvalidImage, result)
    }
}
