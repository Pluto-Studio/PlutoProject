package plutoproject.feature.gallery.core.render

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FrameSamplerTest {
    @Test
    fun `should apply min delay and generate repeats by sample interval`() {
        val sourceFrames = listOf(
            AnimatedSourceFrame(sampleRgbaImage(), delayCentiseconds = 1),
            AnimatedSourceFrame(sampleRgbaImage(), delayCentiseconds = 5),
        )
        val profile = RenderProfile(
            minFrameDelayMillis = 20,
            frameSampleIntervalMillis = 20,
        )

        val result = DefaultFrameSampler.sample(sourceFrames, profile)

        assertEquals(RenderStatus.SUCCEED, result.status)
        assertArrayEquals(intArrayOf(0, 1, 1, 1), result.outToSourceFrameIndex)
        assertEquals(70, result.durationMillis)
    }

    @Test
    fun `should return overflow status when total duration exceeds int`() {
        val sourceFrames = listOf(
            AnimatedSourceFrame(sampleRgbaImage(), delayCentiseconds = Int.MAX_VALUE),
        )
        val profile = RenderProfile(
            minFrameDelayMillis = 20,
            frameSampleIntervalMillis = 1,
        )

        val result = DefaultFrameSampler.sample(sourceFrames, profile)

        assertEquals(RenderStatus.INVALID_RENDERED_DURATION_MILLIS, result.status)
        assertNull(result.outToSourceFrameIndex)
        assertNull(result.durationMillis)
    }
}

private fun sampleRgbaImage(): RgbaImage8888 = RgbaImage8888(
    width = 1,
    height = 1,
    pixels = intArrayOf(0xFFFFFFFF.toInt()),
)
