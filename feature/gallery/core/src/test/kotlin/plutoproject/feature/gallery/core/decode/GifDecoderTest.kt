package plutoproject.feature.gallery.core.decode

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.decode.decoder.GifDecoder
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode

class GifDecoderTest {
    @Test
    fun `gif decoder should compose partial patches into full timeline frames`() = runTest {
        val bytes = createGifBytes(
            logicalWidth = 2,
            logicalHeight = 1,
            frames = listOf(
                GifFrameSpec(image = solidArgbImage(2, 1, 0xFFFF0000.toInt()), left = 0, top = 0, delayCs = 1),
                GifFrameSpec(image = solidArgbImage(1, 1, 0xFF00FF00.toInt()), left = 1, top = 0, delayCs = 7),
            ),
        )

        val result = GifDecoder.decode(bytes, DecodeConstraints())

        assertEquals(DecodeStatus.SUCCEED, result.status)
        val data = result.data as DecodedImage.Animated
        assertEquals(2, data.frames.size)
        assertEquals(intArrayOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()).toList(), data.frames[0].image.pixels.toList())
        assertEquals(intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()).toList(), data.frames[1].image.pixels.toList())
        assertEquals(1, data.frames[0].delayCentiseconds)
        assertEquals(7, data.frames[1].delayCentiseconds)
    }

    @Test
    fun `gif decoder should apply restore-to-background disposal with clipping`() = runTest {
        val bytes = createGifBytes(
            logicalWidth = 2,
            logicalHeight = 1,
            frames = listOf(
                GifFrameSpec(image = solidArgbImage(2, 1, 0xFFFF0000.toInt()), left = 0, top = 0, delayCs = 1),
                GifFrameSpec(
                    image = solidArgbImage(1, 1, 0xFF00FF00.toInt()),
                    left = 1,
                    top = 0,
                    delayCs = 1,
                    disposalMethod = "restoreToBackgroundColor",
                ),
                GifFrameSpec(image = solidArgbImage(1, 1, 0xFF0000FF.toInt()), left = 0, top = 0, delayCs = 1),
            ),
        )

        val result = GifDecoder.decode(bytes, DecodeConstraints())

        assertEquals(DecodeStatus.SUCCEED, result.status)
        val data = result.data as DecodedImage.Animated
        assertEquals(3, data.frames.size)
        assertEquals(intArrayOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()).toList(), data.frames[0].image.pixels.toList())
        assertEquals(intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()).toList(), data.frames[1].image.pixels.toList())
        assertEquals(intArrayOf(0xFF0000FF.toInt(), 0x00000000).toList(), data.frames[2].image.pixels.toList())
    }

    @Test
    fun `gif decoder should apply restore-to-previous disposal`() = runTest {
        val bytes = createGifBytes(
            logicalWidth = 2,
            logicalHeight = 1,
            frames = listOf(
                GifFrameSpec(image = solidArgbImage(2, 1, 0xFFFF0000.toInt()), left = 0, top = 0, delayCs = 1),
                GifFrameSpec(
                    image = solidArgbImage(1, 1, 0xFF00FF00.toInt()),
                    left = 1,
                    top = 0,
                    delayCs = 1,
                    disposalMethod = "restoreToPrevious",
                ),
                GifFrameSpec(image = solidArgbImage(1, 1, 0xFF0000FF.toInt()), left = 0, top = 0, delayCs = 1),
            ),
        )

        val result = GifDecoder.decode(bytes, DecodeConstraints())

        assertEquals(DecodeStatus.SUCCEED, result.status)
        val data = result.data as DecodedImage.Animated
        assertEquals(3, data.frames.size)
        assertEquals(intArrayOf(0xFFFF0000.toInt(), 0xFFFF0000.toInt()).toList(), data.frames[0].image.pixels.toList())
        assertEquals(intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()).toList(), data.frames[1].image.pixels.toList())
        assertEquals(intArrayOf(0xFF0000FF.toInt(), 0xFFFF0000.toInt()).toList(), data.frames[2].image.pixels.toList())
    }

    @Test
    fun `gif decoder should enforce max-frames and max-pixels constraints`() = runTest {
        val bytes = createGifBytes(
            logicalWidth = 2,
            logicalHeight = 2,
            frames = listOf(
                GifFrameSpec(image = solidArgbImage(2, 2, 0xFFFFFFFF.toInt()), left = 0, top = 0, delayCs = 1),
                GifFrameSpec(image = solidArgbImage(2, 2, 0xFF000000.toInt()), left = 0, top = 0, delayCs = 1),
            ),
        )

        val tooManyFrames = GifDecoder.decode(
            bytes,
            DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 16_777_216, maxFrames = 1),
        )
        val tooManyPixels = GifDecoder.decode(
            bytes,
            DecodeConstraints(maxBytes = 1024 * 1024, maxPixels = 3, maxFrames = 10),
        )

        assertEquals(DecodeStatus.TOO_MANY_FRAMES, tooManyFrames.status)
        assertTrue(tooManyFrames.data == null)
        assertEquals(DecodeStatus.IMAGE_TOO_LARGE, tooManyPixels.status)
        assertTrue(tooManyPixels.data == null)
    }
}

private data class GifFrameSpec(
    val image: BufferedImage,
    val left: Int,
    val top: Int,
    val delayCs: Int,
    val disposalMethod: String = "none",
)

private fun solidArgbImage(width: Int, height: Int, argb: Int): BufferedImage =
    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
        for (y in 0 until height) {
            for (x in 0 until width) {
                setRGB(x, y, argb)
            }
        }
    }

private fun createGifBytes(
    logicalWidth: Int,
    logicalHeight: Int,
    frames: List<GifFrameSpec>,
): ByteArray {
    val writer = ImageIO.getImageWritersByFormatName("gif").asSequence().firstOrNull()
        ?: error("GIF writer not available")
    val output = ByteArrayOutputStream()
    val imageOutput = ImageIO.createImageOutputStream(output)
        ?: error("Failed to create image output stream")

    imageOutput.use { ios ->
        writer.output = ios
        val streamMetadata = writer.getDefaultStreamMetadata(null)
        val streamRoot = streamMetadata.getAsTree("javax_imageio_gif_stream_1.0") as IIOMetadataNode
        val screenDescriptor = streamRoot.getElementsByTagName("LogicalScreenDescriptor").item(0) as IIOMetadataNode
        screenDescriptor.setAttribute("logicalScreenWidth", logicalWidth.toString())
        screenDescriptor.setAttribute("logicalScreenHeight", logicalHeight.toString())
        streamMetadata.setFromTree("javax_imageio_gif_stream_1.0", streamRoot)

        writer.prepareWriteSequence(streamMetadata)
        try {
            for (frame in frames) {
                val imageType = ImageTypeSpecifier.createFromRenderedImage(frame.image)
                val metadata = writer.getDefaultImageMetadata(imageType, null)
                val imageRoot = metadata.getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode

                val descriptor = imageRoot.getElementsByTagName("ImageDescriptor").item(0) as IIOMetadataNode
                descriptor.setAttribute("imageLeftPosition", frame.left.toString())
                descriptor.setAttribute("imageTopPosition", frame.top.toString())
                descriptor.setAttribute("imageWidth", frame.image.width.toString())
                descriptor.setAttribute("imageHeight", frame.image.height.toString())

                val graphicControl = imageRoot.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
                graphicControl.setAttribute("delayTime", frame.delayCs.toString())
                graphicControl.setAttribute("disposalMethod", frame.disposalMethod)

                metadata.setFromTree("javax_imageio_gif_image_1.0", imageRoot)
                writer.writeToSequence(IIOImage(frame.image, null, metadata), null)
            }
        } finally {
            writer.endWriteSequence()
            writer.dispose()
        }
    }

    return output.toByteArray()
}
