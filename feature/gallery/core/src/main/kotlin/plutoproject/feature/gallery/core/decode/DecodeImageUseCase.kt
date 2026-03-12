package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import plutoproject.feature.gallery.core.decode.decoder.GifDecoder
import plutoproject.feature.gallery.core.decode.decoder.ImageDecoder
import plutoproject.feature.gallery.core.decode.decoder.StaticImageDecoder
import java.util.logging.Level
import java.util.logging.Logger

class DecodeImageUseCase(
    private val pngDecoder: ImageDecoder = StaticImageDecoder,
    private val jpgDecoder: ImageDecoder = StaticImageDecoder,
    private val webpDecoder: ImageDecoder = StaticImageDecoder,
    private val gifDecoder: ImageDecoder = GifDecoder,
    private val logger: Logger = Logger.getLogger(DecodeImageUseCase::class.java.name),
) {
    suspend fun execute(request: DecodeImageRequest): DecodeResult<DecodedImage> = try {
        checkpoint()

        if (request.bytes.size > request.constraints.maxBytes) {
            return DecodeResult.Failure(DecodeStatus.IMAGE_TOO_LARGE)
        }

        val format = ImageFormatSniffer.sniff(request.bytes, request.fileNameHint)
            ?: return DecodeResult.Failure(DecodeStatus.UNSUPPORTED_FORMAT)
        checkpoint()

        val decoder = when (format) {
            DecodableImageFormat.PNG -> pngDecoder
            DecodableImageFormat.JPEG -> jpgDecoder
            DecodableImageFormat.WEBP -> webpDecoder
            DecodableImageFormat.GIF -> gifDecoder
        }
        val decodeResult = decoder.decode(
            bytes = request.bytes,
            constraints = request.constraints,
        )
        checkpoint()

        val decodedImage = when (decodeResult) {
            is DecodeResult.Failure -> return DecodeResult.Failure(decodeResult.status)
            is DecodeResult.Success -> decodeResult.data
                ?: return DecodeResult.Failure(DecodeStatus.DECODE_FAILED)
        }
        validateDecodedImage(decodedImage, request.constraints)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.log(
            Level.SEVERE,
            "Image decode failed with internal error: bytes=${request.bytes.size}, fileNameHint=${request.fileNameHint}",
            e,
        )
        DecodeResult.Failure(DecodeStatus.DECODE_FAILED)
    }

    private fun validateDecodedImage(
        image: DecodedImage,
        constraints: DecodeConstraints,
    ): DecodeResult<DecodedImage> {
        return when (image) {
            is DecodedImage.Static -> {
                if (!withinPixelLimit(image.image.width, image.image.height, constraints.maxPixels)) {
                    DecodeResult.Failure(DecodeStatus.IMAGE_TOO_LARGE)
                } else {
                    DecodeResult.Success(image)
                }
            }

            is DecodedImage.Animated -> {
                if (image.frames.size > constraints.maxFrames) {
                    return DecodeResult.Failure(DecodeStatus.TOO_MANY_FRAMES)
                }

                val exceedsPixelLimit = image.frames.any {
                    !withinPixelLimit(it.image.width, it.image.height, constraints.maxPixels)
                }
                if (exceedsPixelLimit) {
                    DecodeResult.Failure(DecodeStatus.IMAGE_TOO_LARGE)
                } else {
                    DecodeResult.Success(data = image)
                }
            }
        }
    }
}

private fun withinPixelLimit(width: Int, height: Int, maxPixels: Int): Boolean {
    val pixels = width.toLong() * height.toLong()
    return pixels <= Int.MAX_VALUE.toLong() && pixels <= maxPixels.toLong()
}

private suspend fun checkpoint() {
    currentCoroutineContext().ensureActive()
}
