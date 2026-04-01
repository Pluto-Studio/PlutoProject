package plutoproject.feature.gallery.core.decode.animated

class AnimatedImageMetadata(
    val width: Int,
    val height: Int,
    val frameCount: Int,
    val sourceFrameTimeline: List<SourceFrameMetadata>,
) {
    init {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
        require(frameCount > 0) { "frameCount must be > 0" }
        require(sourceFrameTimeline.size == frameCount) {
            "sourceFrameTimeline size must equal frameCount: size=${sourceFrameTimeline.size}, frameCount=$frameCount"
        }
    }
}
