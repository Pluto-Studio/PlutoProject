package plutoproject.feature.gallery.core.render.framesample

import kotlin.time.Duration
import plutoproject.feature.gallery.core.decode.animated.SourceFrameMetadata

interface FrameSampler {
    fun sample(
        sourceFrameTimeline: List<SourceFrameMetadata>,
        minFrameDuration: Duration,
        outputFrameInterval: Duration,
    ): FrameSamplingResult
}
