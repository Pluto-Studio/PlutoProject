package plutoproject.feature.gallery.core.decode.decoder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import plutoproject.feature.gallery.core.decode.DecodeConstraints
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.DecodeStatus
import plutoproject.feature.gallery.core.decode.DecodedImage
import plutoproject.feature.gallery.core.render.RgbaImage8888
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import java.util.logging.Level
import java.util.logging.Logger

private class DefaultStaticImageDecoder(
    private val logger: Logger,
) : ImageDecoder {
    override suspend fun decode(bytes: ByteArray, constraints: DecodeConstraints): DecodeResult<DecodedImage> = try {
        decodeInternal(bytes = bytes, constraints = constraints)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.log(
            Level.WARNING,
            "Static image decode failed with internal error: bytes=${bytes.size}, maxPixels=${constraints.maxPixels}",
            e,
        )
        DecodeResult.Failure(DecodeStatus.DECODE_FAILED)
    }
}

fun defaultStaticImageDecoder(logger: Logger): ImageDecoder = DefaultStaticImageDecoder(logger = logger)

private suspend fun decodeInternal(
    bytes: ByteArray,
    constraints: DecodeConstraints,
): DecodeResult<DecodedImage> = withContext(Dispatchers.IO) {
    val imageInput = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)) ?: return@withContext null
    imageInput.use { input ->
        val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return@withContext null
        try {
            reader.input = input

            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            val pixelCount = width.toLong() * height.toLong()

            if (width <= 0 || height <= 0 || pixelCount > Int.MAX_VALUE.toLong()) {
                return@withContext DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
            }
            if (pixelCount > constraints.maxPixels.toLong()) {
                return@withContext DecodeResult.Failure(DecodeStatus.IMAGE_TOO_LARGE)
            }

            val image = reader.read(0) ?: return@withContext DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
            val pixels = IntArray(pixelCount.toInt())
            image.getRGB(0, 0, width, height, pixels, 0, width)

            DecodeResult.Success(
                DecodedImage.Static(
                    image = RgbaImage8888(
                        width = width,
                        height = height,
                        pixels = pixels,
                    )
                ),
            )
        } finally {
            reader.dispose()
        }
    }
} ?: DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
