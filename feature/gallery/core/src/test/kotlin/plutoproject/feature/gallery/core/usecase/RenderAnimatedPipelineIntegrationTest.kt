package plutoproject.feature.gallery.core.usecase

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.AnimatedSourceFrame
import plutoproject.feature.gallery.core.render.DefaultAnimatedImageRenderer
import plutoproject.feature.gallery.core.render.DitherAlgorithm
import plutoproject.feature.gallery.core.render.RenderAnimatedImageRequest
import plutoproject.feature.gallery.core.render.RenderProfile
import plutoproject.feature.gallery.core.render.RenderResult
import plutoproject.feature.gallery.core.render.RenderStatus
import plutoproject.feature.gallery.core.render.RepositionMode
import plutoproject.feature.gallery.core.render.RgbaImage8888

class RenderAnimatedPipelineIntegrationTest {
    @Test
    fun `should repeat long-delay frame and keep repeated frame tile indexes identical`() = runTest {
        val frame0 = solidImage(128, 128, argb(255, 255, 0, 0))
        val frame1 = solidImage(128, 128, argb(255, 0, 255, 0))

        val request = RenderAnimatedImageRequest(
            sourceFrames = listOf(
                AnimatedSourceFrame(frame0, delayCentiseconds = 1),
                AnimatedSourceFrame(frame1, delayCentiseconds = 5),
            ),
            mapXBlocks = 1,
            mapYBlocks = 1,
            profile = defaultNoDitherProfile(),
        )

        val result = RenderAnimatedImageUseCase(DefaultAnimatedImageRenderer()).execute(request)

        assertTrue(result is RenderResult.Success)
        val data = (result as RenderResult.Success).imageData!!
        assertEquals(4, data.frameCount)
        assertEquals(70, data.durationMillis)

        val frame0Index = data.tileIndexes[0]
        val frame1Index = data.tileIndexes[1]
        val frame2Index = data.tileIndexes[2]
        val frame3Index = data.tileIndexes[3]
        assertTrue(frame1Index == frame2Index && frame2Index == frame3Index)
        assertTrue(frame0Index != frame1Index)
    }

    @Test
    fun `should dedupe identical tiles across sampled frames`() = runTest {
        val identical = solidImage(128, 128, argb(255, 127, 178, 56))

        val request = RenderAnimatedImageRequest(
            sourceFrames = listOf(
                AnimatedSourceFrame(identical, delayCentiseconds = 2),
                AnimatedSourceFrame(identical, delayCentiseconds = 2),
            ),
            mapXBlocks = 1,
            mapYBlocks = 1,
            profile = defaultNoDitherProfile(),
        )

        val result = RenderAnimatedImageUseCase(DefaultAnimatedImageRenderer()).execute(request)

        assertTrue(result is RenderResult.Success)
        val data = (result as RenderResult.Success).imageData!!
        assertEquals(2, data.frameCount)
        assertEquals(1, data.tilePool.offsets.size - 1)
        assertArrayEquals(shortArrayOf(0, 0), data.tileIndexes)
    }

    @Test
    fun `should return overflow status when sampled duration is too large`() = runTest {
        val request = RenderAnimatedImageRequest(
            sourceFrames = listOf(
                AnimatedSourceFrame(solidImage(1, 1, argb(255, 255, 255, 255)), delayCentiseconds = Int.MAX_VALUE),
            ),
            mapXBlocks = 1,
            mapYBlocks = 1,
            profile = RenderProfile(frameSampleIntervalMillis = 1),
        )

        val result = RenderAnimatedImageUseCase(DefaultAnimatedImageRenderer()).execute(request)

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.INVALID_RENDERED_DURATION_MILLIS, result.status)
    }
}

private fun defaultNoDitherProfile(): RenderProfile = RenderProfile(
    repositionMode = RepositionMode.STRETCH,
    ditherAlgorithm = DitherAlgorithm.NONE,
)

private fun solidImage(width: Int, height: Int, argb: Int): RgbaImage8888 {
    return RgbaImage8888(width, height, IntArray(width * height) { argb })
}

private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
    return ((alpha and 0xFF) shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)
}
