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

object StaticImageDecoder : ImageDecoder {
    override suspend fun decode(bytes: ByteArray, constraints: DecodeConstraints): DecodeResult<DecodedImage> = try {
        val metadata = readImageMetadata(bytes) ?: return DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
        val width = metadata.width
        val height = metadata.height
        val pixelCount = width.toLong() * height.toLong()

        if (width <= 0 || height <= 0 || pixelCount > Int.MAX_VALUE.toLong()) {
            return DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
        }
        if (pixelCount > constraints.maxPixels.toLong()) {
            return DecodeResult.Failure(DecodeStatus.IMAGE_TOO_LARGE)
        }

        val image = readBufferedImage(bytes) ?: return DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
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
    } catch (_: IIOException) {
        DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
    } catch (_: IllegalArgumentException) {
        DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
    }
}

private suspend fun readBufferedImage(bytes: ByteArray) = withContext(Dispatchers.IO) {
    ImageIO.read(ByteArrayInputStream(bytes))
}

private suspend fun readImageMetadata(bytes: ByteArray): ImageMetadata? = withContext(Dispatchers.IO) {
    val imageInput = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)) ?: return@withContext null
    imageInput.use { input ->
        val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return@withContext null
        return@withContext try {
            reader.input = input
            ImageMetadata(
                width = reader.getWidth(0),
                height = reader.getHeight(0),
            )
        } finally {
            reader.dispose()
        }
    }
}

private data class ImageMetadata(
    val width: Int,
    val height: Int,
)
