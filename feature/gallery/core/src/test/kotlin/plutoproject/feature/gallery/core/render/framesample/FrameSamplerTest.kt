package plutoproject.feature.gallery.core.render.framesample

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.decode.animated.SourceFrameMetadata
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

class FrameSamplerTest {
    @Test
    fun `should apply min delay and generate repeats by sample interval`() {
        val frameTimeline = listOf(
            SourceFrameMetadata(sourceFrameIndex = 0, duration = 10.milliseconds),
            SourceFrameMetadata(sourceFrameIndex = 1, duration = 50.milliseconds),
        )

        val result = FixedIntervalFrameSampler.sample(
            sourceFrameTimeline = frameTimeline,
            minFrameDuration = 20.milliseconds,
            outputFrameInterval = 20.milliseconds,
        )

        assertTrue(result is FrameSamplingResult.Success)
        assertArrayEquals(
            intArrayOf(0, 1, 1, 1),
            (result as FrameSamplingResult.Success).outputFrameTimeline.sourceFrameIndexByOutputFrame,
        )
        assertEquals(70.milliseconds, result.outputFrameTimeline.duration)
    }

    @Test
    fun `should return overflow status when total duration exceeds int`() {
        val frameTimeline = listOf(
            SourceFrameMetadata(sourceFrameIndex = 0, duration = Long.MAX_VALUE.nanoseconds),
            SourceFrameMetadata(sourceFrameIndex = 1, duration = Long.MAX_VALUE.nanoseconds),
        )

        val result = FixedIntervalFrameSampler.sample(
            sourceFrameTimeline = frameTimeline,
            minFrameDuration = 1.nanoseconds,
            outputFrameInterval = Long.MAX_VALUE.nanoseconds,
        )

        assertEquals(FrameSamplingResult.DurationOverflow, result)
    }
}
