package plutoproject.feature.gallery.core.decode.animated

import plutoproject.feature.gallery.core.render.PixelBuffer

class AnimatedImageFrame(
    val sourceFrameIndex: Int,
    val pixelBuffer: PixelBuffer,
) {
    init {
        require(sourceFrameIndex >= 0) { "sourceFrameIndex must be >= 0" }
    }
}
