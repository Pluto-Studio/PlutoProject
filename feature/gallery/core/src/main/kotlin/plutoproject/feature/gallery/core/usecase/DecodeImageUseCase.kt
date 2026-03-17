package plutoproject.feature.gallery.core.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import plutoproject.feature.gallery.core.decode.*
import plutoproject.feature.gallery.core.decode.decoder.ImageDecoder
import java.util.logging.Level
import java.util.logging.Logger

class DecodeImageUseCase(
    private val pngDecoder: ImageDecoder,
    private val jpgDecoder: ImageDecoder,
    private val webpDecoder: ImageDecoder,
    private val gifDecoder: ImageDecoder,
    private val logger: Logger,
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
            Level.WARNING,
            "Image decode failed with internal error: bytes=${request.bytes.size}, fileNameHint=${request.fileNameHint}",
            e,
        )
        DecodeResult.Failure(DecodeStatus.DECODE_FAILED)
    }

    private fun validateDecodedImage(
        image: DecodedImage,
        constraints: DecodeConstraints,
    ): DecodeResult<DecodedImage> = when (image) {
        is DecodedImage.Static -> {
            if (!withinPixelLimit(image.image.width, image.image.height, constraints.maxPixels)) {
                DecodeResult.Failure(DecodeStatus.IMAGE_TOO_LARGE)
            } else {
                DecodeResult.Success(image)
            }
        }

        is DecodedImage.Animated -> {
            if (image.source.frameCount > constraints.maxFrames) {
                return DecodeResult.Failure(DecodeStatus.TOO_MANY_FRAMES)
            }

            val exceedsPixelLimit = !withinPixelLimit(
                width = image.source.width,
                height = image.source.height,
                maxPixels = constraints.maxPixels,
            )
            val totalFramePixels = image.source.width.toLong() *
                    image.source.height.toLong() *
                    image.source.frameCount.toLong()
            if (exceedsPixelLimit) {
                DecodeResult.Failure(DecodeStatus.IMAGE_TOO_LARGE)
            } else if (totalFramePixels > constraints.maxTotalFramePixels) {
                DecodeResult.Failure(DecodeStatus.IMAGE_TOO_LARGE)
            } else {
                DecodeResult.Success(data = image)
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
