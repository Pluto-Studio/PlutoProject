package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class StaticImageDecoderTest {
    @Test
    fun `png decoder should decode rgba image with exact pixels`() = runTest {
        val source = BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB).apply {
            setRGB(0, 0, 0x80FF0000.toInt())
            setRGB(1, 0, 0xFF00FF00.toInt())
        }

        val result = StaticImageDecoder.decode(
            bytes = encode(source, "png"),
            constraints = DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
            readerSpi = DecodableImageFormat.PNG.readerSpi
        )

        assertTrue(result is DecodeResult.Success)
        val image = (result as DecodeResult.Success).data
        assertEquals(2, image.width)
        assertEquals(1, image.height)
        assertEquals(0x80FF0000.toInt(), image.pixels[0])
        assertEquals(0xFF00FF00.toInt(), image.pixels[1])
    }

    @Test
    fun `jpeg decoder should decode rgb image into opaque argb`() = runTest {
        val source = BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB).apply {
            setRGB(0, 0, 0x00AA7722)
            setRGB(1, 0, 0x003355CC)
        }

        val result = StaticImageDecoder.decode(
            bytes = encode(source, "jpg"),
            constraints = DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
            readerSpi = DecodableImageFormat.JPEG.readerSpi
        )

        assertTrue(result is DecodeResult.Success)
        val image = (result as DecodeResult.Success).data
        assertEquals(2, image.width)
        assertEquals(1, image.height)
        assertEquals(0xFF000000.toInt(), image.pixels[0] and 0xFF000000.toInt())
        assertEquals(0xFF000000.toInt(), image.pixels[1] and 0xFF000000.toInt())
    }

    @Test
    fun `static decoders should return image-too-large when max-pixels exceeded`() = runTest {
        val pngSource = BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)
        val jpgSource = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        val constraints = DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 3, maxFrames = 500)

        val pngResult = StaticImageDecoder.decode(
            encode(pngSource, "png"), constraints, DecodableImageFormat.PNG.readerSpi
        )
        val jpgResult = StaticImageDecoder.decode(
            encode(jpgSource, "jpg"), constraints, DecodableImageFormat.JPEG.readerSpi
        )

        assertEquals(DecodeResult.ImageTooLarge, pngResult)
        assertEquals(DecodeResult.ImageTooLarge, jpgResult)
    }

    @Test
    fun `jpeg decoder should return invalid-image for malformed bytes`() = runTest {
        val result = StaticImageDecoder.decode(
            bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3),
            constraints = DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 500),
            readerSpi = DecodableImageFormat.JPEG.readerSpi
        )

        assertEquals(DecodeResult.InvalidImage, result)
    }
}

private fun encode(image: BufferedImage, format: String): ByteArray {
    val output = ByteArrayOutputStream()
    val written = ImageIO.write(image, format, output)
    check(written) { "ImageIO cannot write format: $format" }
    return output.toByteArray()
}
