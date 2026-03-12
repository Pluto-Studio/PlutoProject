package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.decode.decoder.ImageDecoder
import plutoproject.feature.gallery.core.render.AnimatedSourceFrame
import plutoproject.feature.gallery.core.render.RgbaImage8888
import plutoproject.feature.gallery.core.usecase.DecodeImageUseCase

class DecodeImageUseCaseTest {
    @Test
    fun `should return image-too-large when bytes exceeds max-bytes`() = runTest {
        var invoked = false
        val useCase = useCaseWith(
            png = { _, _ ->
                invoked = true
                DecodeResult.Failure(DecodeStatus.DECODE_FAILED)
            }
        )

        val result = useCase.execute(
            DecodeImageRequest(
                bytes = pngMagicBytes(),
                constraints = DecodeConstraints(maxBytes = 1, maxPixels = 16_777_216, maxFrames = 500),
            )
        )

        assertTrue(result is DecodeResult.Failure)
        assertEquals(DecodeStatus.IMAGE_TOO_LARGE, result.status)
        assertFalse(invoked)
    }

    @Test
    fun `should return unsupported-format when format cannot be detected`() = runTest {
        var invoked = false
        val useCase = useCaseWith(
            png = { _, _ ->
                invoked = true
                DecodeResult.Failure(DecodeStatus.DECODE_FAILED)
            }
        )

        val result = useCase.execute(DecodeImageRequest(bytes = byteArrayOf(1, 2, 3)))

        assertTrue(result is DecodeResult.Failure)
        assertEquals(DecodeStatus.UNSUPPORTED_FORMAT, result.status)
        assertFalse(invoked)
    }

    @Test
    fun `should choose decoder by detected format`() = runTest {
        var pngInvoked = false
        var gifInvoked = false
        val expected = DecodedImage.Static(sampleRgbaImage(width = 1, height = 1))
        val useCase = DecodeImageUseCase(
            pngDecoder = { _, _ ->
                pngInvoked = true
                DecodeResult.Success(expected)
            },
            jpgDecoder = { _, _ -> DecodeResult.Failure(DecodeStatus.DECODE_FAILED) },
            webpDecoder = { _, _ -> DecodeResult.Failure(DecodeStatus.DECODE_FAILED) },
            gifDecoder = { _, _ ->
                gifInvoked = true
                DecodeResult.Failure(DecodeStatus.DECODE_FAILED)
            },
        )

        val result = useCase.execute(DecodeImageRequest(bytes = pngMagicBytes()))

        assertTrue(result is DecodeResult.Success)
        assertEquals(DecodeStatus.SUCCEED, result.status)
        assertSame(expected, (result as DecodeResult.Success).data)
        assertTrue(pngInvoked)
        assertFalse(gifInvoked)
    }

    @Test
    fun `should pass through decoder failure status`() = runTest {
        val useCase = useCaseWith(
            png = { _, _ -> DecodeResult.Failure(DecodeStatus.INVALID_IMAGE) }
        )

        val result = useCase.execute(DecodeImageRequest(bytes = pngMagicBytes()))

        assertTrue(result is DecodeResult.Failure)
        assertEquals(DecodeStatus.INVALID_IMAGE, result.status)
    }

    @Test
    fun `should return decode-failed when decoder returns succeed with null data`() = runTest {
        val useCase = useCaseWith(
            png = { _, _ ->
                DecodeResult.Success(null)
            }
        )

        val result = useCase.execute(DecodeImageRequest(bytes = pngMagicBytes()))

        assertTrue(result is DecodeResult.Failure)
        assertEquals(DecodeStatus.DECODE_FAILED, result.status)
    }

    @Test
    fun `should return image-too-large when decoded static image exceeds max-pixels`() = runTest {
        val useCase = useCaseWith(
            png = { _, _ ->
                DecodeResult.Success(DecodedImage.Static(sampleRgbaImage(width = 2, height = 2)))
            }
        )

        val result = useCase.execute(
            DecodeImageRequest(
                bytes = pngMagicBytes(),
                constraints = DecodeConstraints(maxBytes = 1024, maxPixels = 3, maxFrames = 500),
            )
        )

        assertTrue(result is DecodeResult.Failure)
        assertEquals(DecodeStatus.IMAGE_TOO_LARGE, result.status)
    }

    @Test
    fun `should return too-many-frames when decoded animated image exceeds max-frames`() = runTest {
        val useCase = useCaseWith(
            gif = { _, _ ->
                DecodeResult.Success(
                    DecodedImage.Animated(
                        frames = listOf(
                            AnimatedSourceFrame(sampleRgbaImage(), delayCentiseconds = 1),
                            AnimatedSourceFrame(sampleRgbaImage(), delayCentiseconds = 1),
                        )
                    ),
                )
            }
        )

        val result = useCase.execute(
            DecodeImageRequest(
                bytes = "GIF89a".encodeToByteArray(),
                constraints = DecodeConstraints(maxBytes = 1024, maxPixels = 16_777_216, maxFrames = 1),
            )
        )

        assertTrue(result is DecodeResult.Failure)
        assertEquals(DecodeStatus.TOO_MANY_FRAMES, result.status)
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
    png: ImageDecoder = ImageDecoder { _, _ -> DecodeResult.Failure(DecodeStatus.DECODE_FAILED) },
    jpg: ImageDecoder = ImageDecoder { _, _ -> DecodeResult.Failure(DecodeStatus.DECODE_FAILED) },
    webp: ImageDecoder = ImageDecoder { _, _ -> DecodeResult.Failure(DecodeStatus.DECODE_FAILED) },
    gif: ImageDecoder = ImageDecoder { _, _ -> DecodeResult.Failure(DecodeStatus.DECODE_FAILED) },
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
