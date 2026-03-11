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
        val image = readBufferedImage(bytes) ?: return DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
        val width = image.width
        val height = image.height
        val pixelCount = width.toLong() * height.toLong()

        if (width <= 0 || height <= 0 || pixelCount > Int.MAX_VALUE.toLong()) {
            return DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
        }
        if (pixelCount > constraints.maxPixels.toLong()) {
            return DecodeResult.failed(DecodeStatus.IMAGE_TOO_LARGE)
        }

        val pixels = IntArray(pixelCount.toInt())
        image.getRGB(0, 0, width, height, pixels, 0, width)

        DecodeResult.succeed(
            DecodedImage.Static(
                image = RgbaImage8888(
                    width = width,
                    height = height,
                    pixels = pixels,
                )
            )
        )
    } catch (_: IIOException) {
        DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
    } catch (_: IllegalArgumentException) {
        DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
    }
}

private suspend fun readBufferedImage(bytes: ByteArray) = withContext(Dispatchers.IO) {
    ImageIO.read(ByteArrayInputStream(bytes))
}
