package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.decode.decoder.defaultGifDecoder
import java.util.Base64
import java.util.logging.Logger

class GifDecoderTest {
    private val decoder = defaultGifDecoder(Logger.getLogger("GifDecoderTest"))

    @Test
    fun `gif decoder should compose partial patches into full timeline frames`() = runTest {
        val result = decoder.decode(decodeBase64(GIF_PATCH_TIMELINE_BASE64), DecodeConstraints())

        assertTrue(result is DecodeResult.Success)
        assertEquals(DecodeStatus.SUCCEED, result.status)
        val data = result.data as DecodedImage.Animated
        val frames = readAllFrames(data.source)
        assertEquals(2, frames.size)
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()), frames[0].image.pixels.toList())
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()), frames[1].image.pixels.toList())
        assertEquals(1, frames[0].delayCentiseconds)
        assertEquals(7, frames[1].delayCentiseconds)
    }

    @Test
    fun `gif decoder should apply restore-to-background disposal with clipping`() = runTest {
        val result = decoder.decode(decodeBase64(GIF_RESTORE_BACKGROUND_CLIPPED_BASE64), DecodeConstraints())

        assertTrue(result is DecodeResult.Success)
        assertEquals(DecodeStatus.SUCCEED, result.status)
        val data = result.data as DecodedImage.Animated
        val frames = readAllFrames(data.source)
        assertEquals(3, frames.size)
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()), frames[0].image.pixels.toList())
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()), frames[1].image.pixels.toList())
        assertEquals(listOf(0xFF0000FF.toInt(), 0x00000000), frames[2].image.pixels.toList())
        assertEquals(0, frames[1].delayCentiseconds)
    }

    @Test
    fun `gif decoder should apply restore-to-previous disposal`() = runTest {
        val result = decoder.decode(decodeBase64(GIF_RESTORE_PREVIOUS_BASE64), DecodeConstraints())

        assertTrue(result is DecodeResult.Success)
        assertEquals(DecodeStatus.SUCCEED, result.status)
        val data = result.data as DecodedImage.Animated
        val frames = readAllFrames(data.source)
        assertEquals(3, frames.size)
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()), frames[0].image.pixels.toList())
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()), frames[1].image.pixels.toList())
        assertEquals(listOf(0xFF0000FF.toInt(), 0xFFFF0000.toInt()), frames[2].image.pixels.toList())
    }

    @Test
    fun `gif decoder should keep canvas pixels when patch pixel is transparent`() = runTest {
        val result = decoder.decode(decodeBase64(GIF_TRANSPARENT_PATCH_OVERLAY_BASE64), DecodeConstraints())

        assertTrue(result is DecodeResult.Success)
        assertEquals(DecodeStatus.SUCCEED, result.status)
        val data = result.data as DecodedImage.Animated
        val frames = readAllFrames(data.source)
        assertEquals(2, frames.size)
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()), frames[0].image.pixels.toList())
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()), frames[1].image.pixels.toList())
    }

    @Test
    fun `gif decoder should enforce max-frames and max-pixels constraints`() = runTest {
        val sample = decodeBase64(GIF_RESTORE_BACKGROUND_CLIPPED_BASE64)

        val tooManyFrames = decoder.decode(
            sample,
            DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 2),
        )
        val tooManyPixels = decoder.decode(
            sample,
            DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 1, maxFrames = 10),
        )

        assertTrue(tooManyFrames is DecodeResult.Failure)
        assertTrue(tooManyPixels is DecodeResult.Failure)
        assertEquals(DecodeStatus.TOO_MANY_FRAMES, tooManyFrames.status)
        assertTrue(tooManyFrames.data == null)
        assertEquals(DecodeStatus.IMAGE_TOO_LARGE, tooManyPixels.status)
        assertTrue(tooManyPixels.data == null)
    }
}

private const val GIF_PATCH_TIMELINE_BASE64 =
    // Generated via jshell + ImageIO GIF writer.
    // logicalScreen=2x1; frames:
    //   1) patch 2x1 @ (0,0), red, delay=1cs, disposal=none
    //   2) patch 1x1 @ (1,0), green, delay=7cs, disposal=none
    "R0lGODlhAgABAPAAAP8AAP8AACH5BAABAAAALAAAAAACAAEAQAgFAAEACAgAIfkEAAcAAAAsAQAAAAEAAQDAAP8AAP8ACAQAAQQEADs="

private const val GIF_RESTORE_BACKGROUND_CLIPPED_BASE64 =
    // Generated via jshell + ImageIO GIF writer.
    // logicalScreen=2x1; frames:
    //   1) patch 2x1 @ (0,0), red, delay=1cs, disposal=none
    //   2) patch 2x1 @ (1,0), green, delay=0cs, disposal=restoreToBackgroundColor
    //      (right half is out-of-bounds to exercise clipping)
    //   3) patch 1x1 @ (0,0), blue, delay=1cs, disposal=none
    "R0lGODlhAgABAPAAAP8AAP8AACH5BAABAAAALAAAAAACAAEAQAgFAAEACAgAIfkECAAAAAAsAQAAAAIAAQDAAP8AAP8ACAUAAQAICAAh+QQAAQAAACwAAAAAAQABAMAAAP8AAP8IBAABBAQAOw=="

private const val GIF_RESTORE_PREVIOUS_BASE64 =
    // Generated via jshell + ImageIO GIF writer.
    // logicalScreen=2x1; frames:
    //   1) patch 2x1 @ (0,0), red, delay=1cs, disposal=none
    //   2) patch 1x1 @ (1,0), green, delay=1cs, disposal=restoreToPrevious
    //   3) patch 1x1 @ (0,0), blue, delay=1cs, disposal=none
    "R0lGODlhAgABAPAAAP8AAP8AACH5BAABAAAALAAAAAACAAEAQAgFAAEACAgAIfkEDAEAAAAsAQAAAAEAAQDAAP8AAP8ACAQAAQQEACH5BAABAAAALAAAAAABAAEAwAAA/wAA/wgEAAEEBAA7"

private const val GIF_TRANSPARENT_PATCH_OVERLAY_BASE64 =
    // Generated via jshell + ImageIO GIF writer.
    // logicalScreen=2x1; frames:
    //   1) patch 2x1 @ (0,0), red, delay=1cs, disposal=none
    //   2) patch 1x1 @ (1,0), transparent, delay=1cs, disposal=none
    // Expected: frame2 keeps previous red pixel at x=1 instead of clearing to transparent.
    "R0lGODlhAgABAPAAAP8AAP8AACH5BAABAAAALAAAAAACAAEAQAgFAAEACAgAIfkEAQEAAAAsAQAAAAEAAQDAAAAAAAAACAQAAQQEADs="

private fun decodeBase64(value: String): ByteArray = Base64.getDecoder().decode(value)

private suspend fun readAllFrames(source: DecodedAnimatedImageSource): List<DecodedAnimatedFrame> {
    val stream = source.openFrameStream()
    try {
        val frames = mutableListOf<DecodedAnimatedFrame>()
        while (true) {
            val frame = stream.nextFrame() ?: break
            frames += frame
        }
        return frames
    } finally {
        stream.close()
    }
}
