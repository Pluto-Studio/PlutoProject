package plutoproject.feature.gallery.core.decode.animated

import kotlin.time.Duration

class SourceFrameMetadata(
    val sourceFrameIndex: Int,
    val duration: Duration,
) {
    init {
        require(sourceFrameIndex >= 0) { "sourceFrameIndex must be >= 0" }
        require(duration >= Duration.ZERO) { "duration must be >= 0" }
    }
}
