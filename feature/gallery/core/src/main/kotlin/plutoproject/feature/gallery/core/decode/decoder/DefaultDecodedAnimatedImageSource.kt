package plutoproject.feature.gallery.core.decode.decoder

import plutoproject.feature.gallery.core.decode.AnimatedFrameTiming
import plutoproject.feature.gallery.core.decode.DecodedAnimatedFrameStream
import plutoproject.feature.gallery.core.decode.DecodedAnimatedImageSource

internal class DefaultDecodedAnimatedImageSource(
    override val width: Int,
    override val height: Int,
    override val frameCount: Int,
    override val frameTimeline: List<AnimatedFrameTiming>,
    internal val opener: suspend () -> DecodedAnimatedFrameStream,
) : DecodedAnimatedImageSource {
    override suspend fun openFrameStream(): DecodedAnimatedFrameStream = opener()
}
