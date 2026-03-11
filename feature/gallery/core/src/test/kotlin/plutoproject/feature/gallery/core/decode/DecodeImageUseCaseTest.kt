package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.decode.decoder.ImageDecoder
import plutoproject.feature.gallery.core.render.AnimatedSourceFrame
import plutoproject.feature.gallery.core.render.RgbaImage8888

class DecodeImageUseCaseTest {
    @Test
    fun `should return image-too-large when bytes exceeds max-bytes`() = runTest {
        var invoked = false
        val useCase = useCaseWith(
            png = { _, _ ->
                invoked = true
                DecodeResult.failed(DecodeStatus.DECODE_FAILED)
            }
        )

        val result = useCase.execute(
            DecodeImageRequest(
                bytes = pngMagicBytes(),
                constraints = DecodeConstraints(maxBytes = 1, maxPixels = 16_777_216, maxFrames = 500),
            )
        )

        assertEquals(DecodeStatus.IMAGE_TOO_LARGE, result.status)
        assertNull(result.data)
        assertFalse(invoked)
    }

    @Test
    fun `should return unsupported-format when format cannot be detected`() = runTest {
        var invoked = false
        val useCase = useCaseWith(
            png = { _, _ ->
                invoked = true
                DecodeResult.failed(DecodeStatus.DECODE_FAILED)
            }
        )

        val result = useCase.execute(DecodeImageRequest(bytes = byteArrayOf(1, 2, 3)))

        assertEquals(DecodeStatus.UNSUPPORTED_FORMAT, result.status)
        assertNull(result.data)
        assertFalse(invoked)
    }

    @Test
    fun `should choose decoder by detected format`() = runTest {
        var pngInvoked = false
        var jpgInvoked = false
        val expected = DecodedImage.Static(sampleRgbaImage(width = 1, height = 1))
        val useCase = DecodeImageUseCase(
            pngDecoder = { _, _ ->
                pngInvoked = true
                DecodeResult.succeed(expected)
            },
            jpgDecoder = { _, _ ->
                jpgInvoked = true
                DecodeResult.failed(DecodeStatus.DECODE_FAILED)
            },
            webpDecoder = { _, _ -> DecodeResult.failed(DecodeStatus.DECODE_FAILED) },
            gifDecoder = { _, _ -> DecodeResult.failed(DecodeStatus.DECODE_FAILED) },
        )

        val result = useCase.execute(DecodeImageRequest(bytes = pngMagicBytes()))

        assertEquals(DecodeStatus.SUCCEED, result.status)
        assertTrue(result.data != null)
        assertSame(expected, result.data)
        assertTrue(pngInvoked)
        assertFalse(jpgInvoked)
    }

    @Test
    fun `should pass through decoder failure status`() = runTest {
        val useCase = useCaseWith(
            png = { _, _ -> DecodeResult.failed(DecodeStatus.INVALID_IMAGE) }
        )

        val result = useCase.execute(DecodeImageRequest(bytes = pngMagicBytes()))

        assertEquals(DecodeStatus.INVALID_IMAGE, result.status)
        assertNull(result.data)
    }

    @Test
    fun `should return decode-failed when decoder returns succeed with null data`() = runTest {
        val useCase = useCaseWith(
            png = { _, _ ->
                DecodeResult(
                    status = DecodeStatus.SUCCEED,
                    data = null,
                )
            }
        )

        val result = useCase.execute(DecodeImageRequest(bytes = pngMagicBytes()))

        assertEquals(DecodeStatus.DECODE_FAILED, result.status)
        assertNull(result.data)
    }

    @Test
    fun `should return image-too-large when decoded static image exceeds max-pixels`() = runTest {
        val useCase = useCaseWith(
            png = { _, _ ->
                DecodeResult.succeed(
                    DecodedImage.Static(sampleRgbaImage(width = 2, height = 2))
                )
            }
        )

        val result = useCase.execute(
            DecodeImageRequest(
                bytes = pngMagicBytes(),
                constraints = DecodeConstraints(maxBytes = 1024, maxPixels = 3, maxFrames = 500),
            )
        )

        assertEquals(DecodeStatus.IMAGE_TOO_LARGE, result.status)
        assertNull(result.data)
    }

    @Test
    fun `should return too-many-frames when decoded animated image exceeds max-frames`() = runTest {
        val useCase = useCaseWith(
            gif = { _, _ ->
                DecodeResult.succeed(
                    DecodedImage.Animated(
                        frames = listOf(
                            AnimatedSourceFrame(sampleRgbaImage(), delayCentiseconds = 1),
                            AnimatedSourceFrame(sampleRgbaImage(), delayCentiseconds = 1),
                        )
                    )
                )
            }
        )

        val result = useCase.execute(
            DecodeImageRequest(
                bytes = "GIF89a".encodeToByteArray(),
                constraints = DecodeConstraints(maxBytes = 1024, maxPixels = 16_777_216, maxFrames = 1),
            )
        )

        assertEquals(DecodeStatus.TOO_MANY_FRAMES, result.status)
        assertNull(result.data)
    }

    @Test
    fun `should rethrow cancellation-exception from decoder`() = runTest {
        val useCase = useCaseWith(
            png = { _, _ -> throw CancellationException("cancel") }
        )

        var cancelled = false
        try {
            useCase.execute(DecodeImageRequest(bytes = pngMagicBytes()))
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
    }
}

private fun useCaseWith(
    png: ImageDecoder = ImageDecoder { _, _ -> DecodeResult.failed(DecodeStatus.DECODE_FAILED) },
    jpg: ImageDecoder = ImageDecoder { _, _ -> DecodeResult.failed(DecodeStatus.DECODE_FAILED) },
    webp: ImageDecoder = ImageDecoder { _, _ -> DecodeResult.failed(DecodeStatus.DECODE_FAILED) },
    gif: ImageDecoder = ImageDecoder { _, _ -> DecodeResult.failed(DecodeStatus.DECODE_FAILED) },
): DecodeImageUseCase = DecodeImageUseCase(
    pngDecoder = png,
    jpgDecoder = jpg,
    webpDecoder = webp,
    gifDecoder = gif,
)

private fun pngMagicBytes(): ByteArray = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47,
    0x0D, 0x0A, 0x1A, 0x0A,
)

private fun sampleRgbaImage(width: Int = 1, height: Int = 1): RgbaImage8888 = RgbaImage8888(
    width = width,
    height = height,
    pixels = IntArray(width * height) { 0xFFFFFFFF.toInt() },
)
