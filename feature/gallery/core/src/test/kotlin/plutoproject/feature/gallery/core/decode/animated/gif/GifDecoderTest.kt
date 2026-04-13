package plutoproject.feature.gallery.core.decode.animated.gif

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.decode.DecodeConstraints
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.GIF_PATCH_TIMELINE_BASE64
import plutoproject.feature.gallery.core.decode.GIF_RESTORE_BACKGROUND_CLIPPED_BASE64
import plutoproject.feature.gallery.core.decode.GIF_RESTORE_PREVIOUS_BASE64
import plutoproject.feature.gallery.core.decode.GIF_TRANSPARENT_PATCH_OVERLAY_BASE64
import plutoproject.feature.gallery.core.decode.animated.AnimatedImageFrame
import plutoproject.feature.gallery.core.decode.animated.AnimatedImageSource
import plutoproject.feature.gallery.core.decode.decodeBase64
import plutoproject.feature.gallery.core.decode.withTempImageFile
import kotlin.time.Duration.Companion.milliseconds

class GifDecoderTest {
    @Test
    fun `gif decoder should compose partial patches into full timeline frames`() = runTest {
        val (data, frames) = withTempImageFile(decodeBase64(GIF_PATCH_TIMELINE_BASE64)) { path ->
            val result = GifDecoder.decode(
                path,
                DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
            )
            assertTrue(result is DecodeResult.Success)
            val data = (result as DecodeResult.Success).data
            data to readAllFrames(data)
        }

        assertEquals(2, frames.size)
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()), frames[0].pixelBuffer.pixels.toList())
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()), frames[1].pixelBuffer.pixels.toList())
        assertEquals(10.milliseconds, data.metadata.sourceFrameTimeline[0].duration)
        assertEquals(70.milliseconds, data.metadata.sourceFrameTimeline[1].duration)
    }

    @Test
    fun `gif decoder should apply restore-to-background disposal with clipping`() = runTest {
        val (data, frames) = withTempImageFile(decodeBase64(GIF_RESTORE_BACKGROUND_CLIPPED_BASE64)) { path ->
            val result = GifDecoder.decode(
                path,
                DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
            )
            assertTrue(result is DecodeResult.Success)
            val data = (result as DecodeResult.Success).data
            data to readAllFrames(data)
        }

        assertEquals(3, frames.size)
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()), frames[0].pixelBuffer.pixels.toList())
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()), frames[1].pixelBuffer.pixels.toList())
        assertEquals(listOf(0xFF0000FF.toInt(), 0x00000000), frames[2].pixelBuffer.pixels.toList())
        assertEquals(0.milliseconds, data.metadata.sourceFrameTimeline[1].duration)
    }

    @Test
    fun `gif decoder should apply restore-to-previous disposal`() = runTest {
        val (data, frames) = withTempImageFile(decodeBase64(GIF_RESTORE_PREVIOUS_BASE64)) { path ->
            val result = GifDecoder.decode(
                path,
                DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
            )
            assertTrue(result is DecodeResult.Success)
            val data = (result as DecodeResult.Success).data
            data to readAllFrames(data)
        }

        assertEquals(3, frames.size)
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()), frames[0].pixelBuffer.pixels.toList())
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()), frames[1].pixelBuffer.pixels.toList())
        assertEquals(listOf(0xFF0000FF.toInt(), 0xFFFF0000.toInt()), frames[2].pixelBuffer.pixels.toList())
    }

    @Test
    fun `gif decoder should keep canvas pixels when patch pixel is transparent`() = runTest {
        val frames = withTempImageFile(decodeBase64(GIF_TRANSPARENT_PATCH_OVERLAY_BASE64)) { path ->
            val result = GifDecoder.decode(
                path,
                DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
            )
            assertTrue(result is DecodeResult.Success)
            readAllFrames((result as DecodeResult.Success).data)
        }

        assertEquals(2, frames.size)
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()), frames[0].pixelBuffer.pixels.toList())
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()), frames[1].pixelBuffer.pixels.toList())
    }

    @Test
    fun `gif decoder should enforce max-frames and max-pixels constraints`() = runTest {
        val sample = decodeBase64(GIF_RESTORE_BACKGROUND_CLIPPED_BASE64)

        val (tooManyFrames, tooManyPixels) = withTempImageFile(sample) { path ->
            GifDecoder.decode(
                path,
                DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 2),
            ) to GifDecoder.decode(
                path,
                DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 1, maxFrames = 10),
            )
        }

        assertEquals(DecodeResult.TooManyFrames, tooManyFrames)
        assertEquals(DecodeResult.ImageTooLarge, tooManyPixels)
    }

    @Test
    fun `gif decoder should return invalid-image for malformed bytes`() = runTest {
        val result = withTempImageFile("GIF89a".encodeToByteArray()) { path ->
            GifDecoder.decode(
                path = path,
                constraints = DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
            )
        }

        assertEquals(DecodeResult.InvalidImage, result)
    }
}

private suspend fun readAllFrames(source: AnimatedImageSource): List<AnimatedImageFrame> {
    val stream = source.openFrameStream()
    try {
        val frames = mutableListOf<AnimatedImageFrame>()
        while (true) {
            val frame = stream.nextFrame() ?: break
            frames += frame
        }
        return frames
    } finally {
        stream.close()
    }
}
