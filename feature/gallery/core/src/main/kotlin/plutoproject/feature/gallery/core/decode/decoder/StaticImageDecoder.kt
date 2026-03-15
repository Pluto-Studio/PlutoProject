package plutoproject.feature.gallery.core.decode.decoder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import plutoproject.feature.gallery.core.decode.DecodeConstraints
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.DecodeStatus
import plutoproject.feature.gallery.core.decode.DecodedImage
import plutoproject.feature.gallery.core.render.RgbaImage8888
import java.io.ByteArrayInputStream
import javax.imageio.IIOException
import javax.imageio.ImageIO

private class DefaultStaticImageDecoder : ImageDecoder {
    override suspend fun decode(bytes: ByteArray, constraints: DecodeConstraints): DecodeResult<DecodedImage> = try {
        decodeInternal(bytes = bytes, constraints = constraints)
    } catch (_: IIOException) {
        DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
    } catch (_: IllegalArgumentException) {
        DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
    }
}

fun defaultStaticImageDecoder(): ImageDecoder = DefaultStaticImageDecoder()

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
