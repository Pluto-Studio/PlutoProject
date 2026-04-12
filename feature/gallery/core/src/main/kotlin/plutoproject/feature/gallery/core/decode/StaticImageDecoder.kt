package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import plutoproject.feature.gallery.core.render.PixelBuffer
import java.io.ByteArrayInputStream
import java.io.EOFException
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.imageio.spi.ImageInputStreamSpi
import javax.imageio.spi.ImageReaderSpi
import javax.imageio.stream.ImageInputStream

object StaticImageDecoder {
    suspend fun decode(
        bytes: ByteArray,
        constraints: DecodeConstraints,
        inputStreamSpi: ImageInputStreamSpi? = null,
        readerSpi: ImageReaderSpi? = null,
    ): DecodeResult<PixelBuffer> = try {
        if (bytes.size > constraints.maxBytes) {
            return DecodeResult.ImageTooLarge
        }

        val imageInput = createImageInput(bytes, inputStreamSpi) ?: return DecodeResult.UnsupportedFormat

        imageInput.use { input ->
            decodeImageInput(input, constraints, readerSpi)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: EOFException) {
        DecodeResult.InvalidImage
    } catch (_: IIOException) {
        DecodeResult.InvalidImage
    } catch (e: Throwable) {
        DecodeResult.UnknownFailure(e)
    }
}

private suspend fun decodeImageInput(
    input: ImageInputStream,
    constraints: DecodeConstraints,
    readerSpi: ImageReaderSpi?,
): DecodeResult<PixelBuffer> {
    val reader = createReader(input, readerSpi) ?: return DecodeResult.UnsupportedFormat

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

private suspend fun createImageInput(
    bytes: ByteArray,
    inputStreamSpi: ImageInputStreamSpi?,
): ImageInputStream? = withContext(Dispatchers.IO) {
    inputStreamSpi?.createInputStreamInstance(bytes, false, null)
        ?: ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
}

private suspend fun createReader(
    input: ImageInputStream,
    readerSpi: ImageReaderSpi?,
) = withContext(Dispatchers.IO) {
    readerSpi?.createReaderInstance()
        ?: ImageIO.getImageReaders(input).asSequence().firstOrNull()?.also {
            input.seek(0)
        }
}
