package plutoproject.feature.gallery.core.render

sealed class FrameSampleResult {
    abstract val status: RenderStatus

    class Failure(override val status: RenderStatus) : FrameSampleResult() {
        init {
            require(status != RenderStatus.SUCCEED) {
                "failed status cannot be SUCCEED"
            }
        }
    }

    class Success(
        /**
         * 输出帧到源帧的映射表：```sourceFrameIndex = outToSourceFrameIndex[outFrameIndex]```。
         *
         * 说明：
         * - 这是按 `outFrameIndex` 顺序排列的稠密查表数组，不是用于存储原始帧数据。
         * - 重复值表示“同一 source frame 被重复输出”（用于表达长 delay）。
         */
        val outToSourceFrameIndex: IntArray,
        val durationMillis: Int,
    ) : FrameSampleResult() {
        override val status: RenderStatus = RenderStatus.SUCCEED
    }
}

fun interface FrameSampler {
    fun sample(sourceFrames: List<AnimatedSourceFrame>, profile: RenderProfile): FrameSampleResult
}

fun defaultFrameSampler(): FrameSampler = DefaultFrameSampler

internal object DefaultFrameSampler : FrameSampler {
    override fun sample(sourceFrames: List<AnimatedSourceFrame>, profile: RenderProfile): FrameSampleResult {
        val outFrameIndexes = ArrayList<Int>(sourceFrames.size)
        var durationMillisLong = 0L

        var srcFrameIndex = 0
        while (srcFrameIndex < sourceFrames.size) {
            val delayCentiseconds = sourceFrames[srcFrameIndex].delayCentiseconds
            val effectiveDelayMillisLong = maxOf(
                delayCentiseconds.toLong() * 10L,
                profile.minFrameDelayMillis.toLong(),
            )

            durationMillisLong += effectiveDelayMillisLong
            if (durationMillisLong > Int.MAX_VALUE.toLong()) {
                return FrameSampleResult.Failure(RenderStatus.INVALID_RENDERED_DURATION_MILLIS)
            }

            val repeatLong = (effectiveDelayMillisLong + profile.frameSampleIntervalMillis - 1L) /
                profile.frameSampleIntervalMillis.toLong()
            if (repeatLong <= 0L) {
                return FrameSampleResult.Failure(RenderStatus.INVALID_RENDERED_FRAME_COUNT)
            }

            if (outFrameIndexes.size.toLong() + repeatLong > Int.MAX_VALUE.toLong()) {
                return FrameSampleResult.Failure(RenderStatus.TILE_INDEXES_LENGTH_OVERFLOW)
            }

            var repeat = 0L
            while (repeat < repeatLong) {
                outFrameIndexes.add(srcFrameIndex)
                repeat++
            }

            srcFrameIndex++
        }

        return FrameSampleResult.Success(
            outToSourceFrameIndex = outFrameIndexes.toIntArray(),
            durationMillis = durationMillisLong.toInt(),
        )
    }
}
