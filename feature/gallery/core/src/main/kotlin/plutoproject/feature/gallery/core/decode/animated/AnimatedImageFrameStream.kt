package plutoproject.feature.gallery.core.decode.animated

interface AnimatedImageFrameStream : AutoCloseable {
    suspend fun nextFrame(): AnimatedImageFrame?
}
