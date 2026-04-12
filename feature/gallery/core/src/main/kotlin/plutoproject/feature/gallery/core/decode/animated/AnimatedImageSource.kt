package plutoproject.feature.gallery.core.decode.animated

class AnimatedImageSource(
    val metadata: AnimatedImageMetadata,
    private val frameStreamOpener: suspend () -> AnimatedImageFrameStream,
) {
    suspend fun openFrameStream(): AnimatedImageFrameStream = frameStreamOpener()
}
