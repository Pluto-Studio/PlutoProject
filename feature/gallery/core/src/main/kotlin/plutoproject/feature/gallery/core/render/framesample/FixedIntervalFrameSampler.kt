package plutoproject.feature.gallery.core.render.framesample

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import plutoproject.feature.gallery.core.decode.animated.SourceFrameMetadata

object FixedIntervalFrameSampler : FrameSampler {
    override fun sample(
        sourceFrameTimeline: List<SourceFrameMetadata>,
        minFrameDuration: Duration,
        outputFrameInterval: Duration,
    ): FrameSamplingResult {
        require(minFrameDuration > Duration.ZERO) { "minFrameDuration must be > 0" }
        require(outputFrameInterval > Duration.ZERO) { "outputFrameInterval must be > 0" }

        val sourceFrameIndexByOutputFrame = ArrayList<Int>(sourceFrameTimeline.size)
        val minFrameDurationNanoseconds = minFrameDuration.inWholeNanoseconds
        val outputFrameIntervalNanoseconds = outputFrameInterval.inWholeNanoseconds
        var durationNanoseconds = 0L

        for (timing in sourceFrameTimeline) {
            val effectiveDurationNanoseconds = maxOf(timing.duration.inWholeNanoseconds, minFrameDurationNanoseconds)

            durationNanoseconds = try {
                Math.addExact(durationNanoseconds, effectiveDurationNanoseconds)
            } catch (_: ArithmeticException) {
                return FrameSamplingResult.DurationOverflow
            }

            val repeatCount = calcRepeatCount(effectiveDurationNanoseconds, outputFrameIntervalNanoseconds)
            if (sourceFrameIndexByOutputFrame.size.toLong() + repeatCount > Int.MAX_VALUE.toLong()) {
                return FrameSamplingResult.OutputFrameCountOverflow
            }

            var repeatIndex = 0L
            while (repeatIndex < repeatCount) {
                sourceFrameIndexByOutputFrame += timing.sourceFrameIndex
                repeatIndex++
            }
        }

        return FrameSamplingResult.Success(
            OutputFrameTimeline(
                sourceFrameIndexByOutputFrame = sourceFrameIndexByOutputFrame.toIntArray(),
                duration = durationNanoseconds.nanoseconds,
            )
        )
    }
}

private fun calcRepeatCount(
    effectiveDurationNanoseconds: Long,
    outputFrameIntervalNanoseconds: Long,
): Long {
    return (effectiveDurationNanoseconds + outputFrameIntervalNanoseconds - 1L) / outputFrameIntervalNanoseconds
}
