package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import plutoproject.feature.gallery.core.render.PixelBuffer
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

object StaticImageDecoder {
    suspend fun decode(bytes: ByteArray, constraints: DecodeConstraints): DecodeResult<PixelBuffer> = try {
        if (bytes.size > constraints.maxBytes) {
            return DecodeResult.ImageTooLarge
        }

        val imageInput = withContext(Dispatchers.IO) {
            ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        } ?: return DecodeResult.UnsupportedFormat

        imageInput.use { input ->
            decodeImageInput(input, constraints)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        DecodeResult.UnknownFailure(e)
    }
}

private suspend fun decodeImageInput(
    input: ImageInputStream,
    constraints: DecodeConstraints
): DecodeResult<PixelBuffer> {
    val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull()
        ?: return DecodeResult.InvalidImage
    try {
        reader.input = input

        val (width, height) = withContext(Dispatchers.IO) {
            reader.getWidth(0) to reader.getHeight(0)
        }
        val pixelCount = width.toLong() * height.toLong()

        if (width <= 0 || height <= 0 || pixelCount > Int.MAX_VALUE.toLong()) {
            return DecodeResult.InvalidImage
        }

        if (pixelCount > constraints.maxPixels.toLong()) {
            return DecodeResult.ImageTooLarge
        }

        val image = withContext(Dispatchers.IO) {
            reader.read(0)
        } ?: return DecodeResult.InvalidImage
        val pixels = image.getRGB(0, 0, width, height, IntArray(pixelCount.toInt()), 0, width)

        return DecodeResult.Success(PixelBuffer(width, height, pixels))
    } finally {
        reader.dispose()
    }
}
