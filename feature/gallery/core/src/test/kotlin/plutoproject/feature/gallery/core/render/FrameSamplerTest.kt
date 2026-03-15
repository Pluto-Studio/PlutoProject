package plutoproject.feature.gallery.core.render

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.decode.AnimatedFrameTiming

class FrameSamplerTest {
    @Test
    fun `should apply min delay and generate repeats by sample interval`() {
        val frameTimeline = listOf(
            AnimatedFrameTiming(sourceFrameIndex = 0, delayCentiseconds = 1),
            AnimatedFrameTiming(sourceFrameIndex = 1, delayCentiseconds = 5),
        )
        val profile = RenderProfile(
            minFrameDelayMillis = 20,
            frameSampleIntervalMillis = 20,
        )

        val result = DefaultFrameSampler.sample(frameTimeline, profile)

        assertTrue(result is FrameSampleResult.Success)
        assertEquals(RenderStatus.SUCCEED, result.status)
        assertArrayEquals(intArrayOf(0, 1, 1, 1), (result as FrameSampleResult.Success).outToSourceFrameIndex)
        assertEquals(70, result.durationMillis)
    }

    @Test
    fun `should return overflow status when total duration exceeds int`() {
        val frameTimeline = listOf(
            AnimatedFrameTiming(sourceFrameIndex = 0, delayCentiseconds = Int.MAX_VALUE),
        )
        val profile = RenderProfile(
            minFrameDelayMillis = 20,
            frameSampleIntervalMillis = 1,
        )

        val result = DefaultFrameSampler.sample(frameTimeline, profile)

        assertTrue(result is FrameSampleResult.Failure)
        assertEquals(RenderStatus.INVALID_RENDERED_DURATION_MILLIS, result.status)
    }
}
