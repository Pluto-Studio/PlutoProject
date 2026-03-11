package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.decode.decoder.StaticImageDecoder
import java.util.Base64

class WebpDecoderTest {
    @Test
    fun `static image decoder should decode embedded webp sample`() = runTest {
        val result = StaticImageDecoder.decode(
            bytes = decodeBase64(WEBP_1X1_TRANSPARENT_BASE64),
            constraints = DecodeConstraints(),
        )

        assertEquals(DecodeStatus.SUCCEED, result.status)
        val image = (result.data as DecodedImage.Static).image
        assertEquals(1, image.width)
        assertEquals(1, image.height)
        assertEquals(0, image.pixels[0] ushr 24)
    }

    @Test
    fun `static image decoder should return invalid-image for malformed webp bytes`() = runTest {
        val result = StaticImageDecoder.decode(
            bytes = byteArrayOf(
                'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
                0, 0, 0, 0,
                'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte(),
            ),
            constraints = DecodeConstraints(),
        )

        assertEquals(DecodeStatus.INVALID_IMAGE, result.status)
        assertNull(result.data)
    }
}

private const val WEBP_1X1_TRANSPARENT_BASE64 =
    // 1x1 transparent WebP sample used for decode smoke test.
    // Source: public minimal sample string (RIFF/WEBP, VP8L) from Dirask snippet.
    // Expected decoded image: width=1, height=1, alpha=0.
    "UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA=="

private fun decodeBase64(value: String): ByteArray = Base64.getDecoder().decode(value)
