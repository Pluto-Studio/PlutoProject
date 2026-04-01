package plutoproject.feature.gallery.core.render.framesample

sealed interface FrameSamplingResult {
    class Success(
        val outputFrameTimeline: OutputFrameTimeline,
    ) : FrameSamplingResult

    data object DurationOverflow : FrameSamplingResult
    data object OutputFrameCountOverflow : FrameSamplingResult
}
