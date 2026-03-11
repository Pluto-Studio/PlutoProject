package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.decode.decoder.StaticImageDecoder

class WebpDecoderTest {
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
