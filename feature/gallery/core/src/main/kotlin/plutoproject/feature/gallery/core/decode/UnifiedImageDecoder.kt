package plutoproject.feature.gallery.core.decode

import plutoproject.feature.gallery.core.decode.animated.AnimatedImageSource
import plutoproject.feature.gallery.core.decode.animated.gif.GifDecoder
import plutoproject.feature.gallery.core.render.PixelBuffer
import java.nio.file.Path

object UnifiedImageDecoder {
    sealed interface Result {
        val result: DecodeResult<*>

        data class StaticSuccess(override val result: DecodeResult<PixelBuffer>) : Result
        data class AnimatedSuccess(override val result: DecodeResult<AnimatedImageSource>) : Result
        data class Failure(override val result: DecodeResult<*>) : Result
    }

    suspend fun decode(path: Path, constraints: DecodeConstraints): Result {
        val format = ImageFormatSniffer.sniff(path)
            ?: return Result.Failure(DecodeResult.UnsupportedFormat)

        return when (format) {
            SupportedImageFormat.Gif -> GifDecoder.decode(path, constraints).wrapAnimated()
            else -> StaticImageDecoder.decode(
                path = path,
                constraints = constraints,
                inputStreamSpi = format.inputStreamSpi,
                readerSpi = format.readerSpi,
            ).wrapStatic()
        }
    }
}

private fun DecodeResult<PixelBuffer>.wrapStatic(): UnifiedImageDecoder.Result {
    return when (this) {
        is DecodeResult.Success -> UnifiedImageDecoder.Result.StaticSuccess(this)
        else -> UnifiedImageDecoder.Result.Failure(this)
    }
}

private fun DecodeResult<AnimatedImageSource>.wrapAnimated(): UnifiedImageDecoder.Result {
    return when (this) {
        is DecodeResult.Success -> UnifiedImageDecoder.Result.AnimatedSuccess(this)
        else -> UnifiedImageDecoder.Result.Failure(this)
    }
}
