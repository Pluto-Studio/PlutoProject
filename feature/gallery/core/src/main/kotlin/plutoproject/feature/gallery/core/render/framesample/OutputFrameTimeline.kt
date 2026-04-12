package plutoproject.feature.gallery.core.render.framesample

import kotlin.time.Duration

class OutputFrameTimeline(
    val sourceFrameIndexByOutputFrame: IntArray,
    val duration: Duration,
) {
    init {
        require(sourceFrameIndexByOutputFrame.isNotEmpty()) { "sourceFrameIndexByOutputFrame must not be empty" }
        require(duration > Duration.ZERO) { "duration must be > 0" }

        var index = 0
        while (index < sourceFrameIndexByOutputFrame.size) {
            require(sourceFrameIndexByOutputFrame[index] >= 0) {
                "sourceFrameIndexByOutputFrame[$index] must be >= 0"
            }
            index++
        }
    }

    val frameCount: Int
        get() = sourceFrameIndexByOutputFrame.size
}
