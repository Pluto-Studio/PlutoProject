package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.decode.decoder.StaticImageDecoder
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
            constraints = DecodeConstraints(),
        )

        assertEquals(DecodeStatus.SUCCEED, result.status)
        val image = (result.data as DecodedImage.Static).image
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
            constraints = DecodeConstraints(),
        )

        assertEquals(DecodeStatus.SUCCEED, result.status)
        val image = (result.data as DecodedImage.Static).image
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

        val pngResult = StaticImageDecoder.decode(encode(pngSource, "png"), constraints)
        val jpgResult = StaticImageDecoder.decode(encode(jpgSource, "jpg"), constraints)

        assertEquals(DecodeStatus.IMAGE_TOO_LARGE, pngResult.status)
        assertEquals(DecodeStatus.IMAGE_TOO_LARGE, jpgResult.status)
    }

    @Test
    fun `jpeg decoder should return invalid-image for malformed bytes`() = runTest {
        val result = StaticImageDecoder.decode(
            bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3),
            constraints = DecodeConstraints(),
        )

        assertEquals(DecodeStatus.INVALID_IMAGE, result.status)
        assertTrue(result.data == null)
    }
}

private fun encode(image: BufferedImage, format: String): ByteArray {
    val output = ByteArrayOutputStream()
    val written = ImageIO.write(image, format, output)
    check(written) { "ImageIO cannot write format: $format" }
    return output.toByteArray()
}
