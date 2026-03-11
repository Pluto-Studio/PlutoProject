package plutoproject.feature.gallery.core.decode.decoder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import plutoproject.feature.gallery.core.decode.DecodeConstraints
import plutoproject.feature.gallery.core.decode.DecodeResult
import plutoproject.feature.gallery.core.decode.DecodeStatus
import plutoproject.feature.gallery.core.decode.DecodedImage
import plutoproject.feature.gallery.core.render.AnimatedSourceFrame
import plutoproject.feature.gallery.core.render.RgbaImage8888
import java.io.ByteArrayInputStream
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.metadata.IIOMetadata
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node

object GifDecoder : ImageDecoder {
    override suspend fun decode(bytes: ByteArray, constraints: DecodeConstraints): DecodeResult<DecodedImage> = try {
        decodeInternal(bytes = bytes, constraints = constraints)
    } catch (_: IIOException) {
        DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
    } catch (_: IllegalArgumentException) {
        DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
    }
}

private suspend fun decodeInternal(
    bytes: ByteArray,
    constraints: DecodeConstraints,
): DecodeResult<DecodedImage> = withContext(Dispatchers.IO) {
    val imageInput = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        ?: return@withContext DecodeResult.failed(DecodeStatus.INVALID_IMAGE)

    imageInput.use { input ->
        val reader = ImageIO.getImageReadersByFormatName("gif").asSequence().firstOrNull()
            ?: return@withContext DecodeResult.failed(DecodeStatus.DECODE_FAILED)
        try {
            reader.input = input
            return@withContext decodeGifFrames(reader, constraints)
        } finally {
            reader.dispose()
        }
    }
}

private suspend fun decodeGifFrames(
    reader: ImageReader,
    constraints: DecodeConstraints,
): DecodeResult<DecodedImage> {
    val screenSize = readLogicalScreenSize(reader)
    val width = screenSize.width
    val height = screenSize.height
    if (width <= 0 || height <= 0) {
        return DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
    }

    val pixelCount = width.toLong() * height.toLong()
    if (pixelCount > Int.MAX_VALUE.toLong()) {
        return DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
    }
    if (pixelCount > constraints.maxPixels.toLong()) {
        return DecodeResult.failed(DecodeStatus.IMAGE_TOO_LARGE)
    }

    val frameCount = withContext(Dispatchers.IO) {
        reader.getNumImages(true)
    }
    if (frameCount <= 0) {
        return DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
    }
    if (frameCount > constraints.maxFrames) {
        return DecodeResult.failed(DecodeStatus.TOO_MANY_FRAMES)
    }

    val canvas = IntArray(pixelCount.toInt())
    val frames = ArrayList<AnimatedSourceFrame>(frameCount)

    for (index in 0 until frameCount) {
        checkpoint()

        val metadata = withContext(Dispatchers.IO) {
            reader.getImageMetadata(index)
        }
        val descriptor = readFrameDescriptor(metadata)
        val delayCentiseconds = readDelayCentiseconds(metadata)
        val disposalMethod = readDisposalMethod(metadata)

        val patchImage = withContext(Dispatchers.IO) {
            reader.read(index)
        } ?: return DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
        val patchWidth = patchImage.width
        val patchHeight = patchImage.height
        if (patchWidth <= 0 || patchHeight <= 0) {
            return DecodeResult.failed(DecodeStatus.INVALID_IMAGE)
        }

        val patchPixels = IntArray(patchWidth * patchHeight)
        patchImage.getRGB(0, 0, patchWidth, patchHeight, patchPixels, 0, patchWidth)

        val patchRect = resolvePatchRect(descriptor, patchWidth, patchHeight)
        val snapshotBeforeDraw = if (disposalMethod == GifDisposalMethod.RESTORE_TO_PREVIOUS) {
            canvas.copyOf()
        } else {
            null
        }

        drawPatch(
            canvas = canvas,
            canvasWidth = width,
            canvasHeight = height,
            patchPixels = patchPixels,
            patchWidth = patchWidth,
            patchHeight = patchHeight,
            patchLeft = patchRect.left,
            patchTop = patchRect.top,
        )

        frames += AnimatedSourceFrame(
            image = RgbaImage8888(width = width, height = height, pixels = canvas.copyOf()),
            delayCentiseconds = delayCentiseconds,
        )

        applyDisposal(
            method = disposalMethod,
            canvas = canvas,
            canvasWidth = width,
            canvasHeight = height,
            patchRect = patchRect,
            snapshotBeforeDraw = snapshotBeforeDraw,
        )
    }

    return DecodeResult.succeed(DecodedImage.Animated(frames))
}

private fun readLogicalScreenSize(reader: ImageReader): IntSize {
    val streamMetadata = reader.streamMetadata ?: return IntSize(width = -1, height = -1)
    val root = runCatching { streamMetadata.getAsTree("javax_imageio_gif_stream_1.0") }.getOrNull()
        ?: return IntSize(width = -1, height = -1)
    val logicalScreen = root.findFirstNode("LogicalScreenDescriptor") ?: return IntSize(width = -1, height = -1)
    val attrs = logicalScreen.attributes
    return IntSize(
        width = attrs.readInt("logicalScreenWidth") ?: -1,
        height = attrs.readInt("logicalScreenHeight") ?: -1,
    )
}

private fun readFrameDescriptor(metadata: IIOMetadata): GifFrameDescriptor {
    val root = runCatching { metadata.getAsTree("javax_imageio_gif_image_1.0") }.getOrNull()
        ?: return GifFrameDescriptor(left = 0, top = 0, width = -1, height = -1)
    val imageDescriptor = root.findFirstNode("ImageDescriptor")
        ?: return GifFrameDescriptor(left = 0, top = 0, width = -1, height = -1)
    val attrs = imageDescriptor.attributes
    return GifFrameDescriptor(
        left = attrs.readInt("imageLeftPosition") ?: 0,
        top = attrs.readInt("imageTopPosition") ?: 0,
        width = attrs.readInt("imageWidth") ?: -1,
        height = attrs.readInt("imageHeight") ?: -1,
    )
}

private fun readDelayCentiseconds(metadata: IIOMetadata): Int {
    val root = runCatching { metadata.getAsTree("javax_imageio_gif_image_1.0") }.getOrNull() ?: return 0
    val graphicControl = root.findFirstNode("GraphicControlExtension") ?: return 0
    return graphicControl.attributes.readInt("delayTime")?.coerceAtLeast(0) ?: 0
}

private fun readDisposalMethod(metadata: IIOMetadata): GifDisposalMethod {
    val root = runCatching { metadata.getAsTree("javax_imageio_gif_image_1.0") }.getOrNull()
        ?: return GifDisposalMethod.NONE
    val graphicControl = root.findFirstNode("GraphicControlExtension") ?: return GifDisposalMethod.NONE
    return when (graphicControl.attributes.readString("disposalMethod")) {
        "restoreToBackgroundColor" -> GifDisposalMethod.RESTORE_TO_BACKGROUND
        "restoreToPrevious" -> GifDisposalMethod.RESTORE_TO_PREVIOUS
        "none", "doNotDispose" -> GifDisposalMethod.NONE
        else -> GifDisposalMethod.NONE
    }
}

private fun resolvePatchRect(
    descriptor: GifFrameDescriptor,
    patchWidth: Int,
    patchHeight: Int,
): IntRect {
    val width = if (descriptor.width > 0) descriptor.width else patchWidth
    val height = if (descriptor.height > 0) descriptor.height else patchHeight
    return IntRect(
        left = descriptor.left,
        top = descriptor.top,
        width = width,
        height = height,
    )
}

private fun drawPatch(
    canvas: IntArray,
    canvasWidth: Int,
    canvasHeight: Int,
    patchPixels: IntArray,
    patchWidth: Int,
    patchHeight: Int,
    patchLeft: Int,
    patchTop: Int,
) {
    val clippedLeft = patchLeft.coerceIn(0, canvasWidth)
    val clippedTop = patchTop.coerceIn(0, canvasHeight)
    val clippedRight = (patchLeft + patchWidth).coerceIn(0, canvasWidth)
    val clippedBottom = (patchTop + patchHeight).coerceIn(0, canvasHeight)

    if (clippedRight <= clippedLeft || clippedBottom <= clippedTop) {
        return
    }

    for (y in clippedTop until clippedBottom) {
        val srcY = y - patchTop
        val srcBase = srcY * patchWidth
        val dstBase = y * canvasWidth
        for (x in clippedLeft until clippedRight) {
            val srcX = x - patchLeft
            val srcPixel = patchPixels[srcBase + srcX]
            if ((srcPixel ushr 24) != 0) {
                canvas[dstBase + x] = srcPixel
            }
        }
    }
}

private fun applyDisposal(
    method: GifDisposalMethod,
    canvas: IntArray,
    canvasWidth: Int,
    canvasHeight: Int,
    patchRect: IntRect,
    snapshotBeforeDraw: IntArray?,
) {
    when (method) {
        GifDisposalMethod.NONE -> Unit
        GifDisposalMethod.RESTORE_TO_BACKGROUND -> clearRect(canvas, canvasWidth, canvasHeight, patchRect)
        GifDisposalMethod.RESTORE_TO_PREVIOUS -> {
            if (snapshotBeforeDraw != null && snapshotBeforeDraw.size == canvas.size) {
                snapshotBeforeDraw.copyInto(canvas)
            }
        }
    }
}

private fun clearRect(canvas: IntArray, canvasWidth: Int, canvasHeight: Int, rect: IntRect) {
    val clippedLeft = rect.left.coerceIn(0, canvasWidth)
    val clippedTop = rect.top.coerceIn(0, canvasHeight)
    val clippedRight = (rect.left + rect.width).coerceIn(0, canvasWidth)
    val clippedBottom = (rect.top + rect.height).coerceIn(0, canvasHeight)

    if (clippedRight <= clippedLeft || clippedBottom <= clippedTop) {
        return
    }

    for (y in clippedTop until clippedBottom) {
        val base = y * canvasWidth
        for (x in clippedLeft until clippedRight) {
            canvas[base + x] = 0x00000000
        }
    }
}

private fun Node.findFirstNode(nodeName: String): Node? {
    if (this.nodeName == nodeName) {
        return this
    }
    val children = childNodes
    for (index in 0 until children.length) {
        val found = children.item(index).findFirstNode(nodeName)
        if (found != null) {
            return found
        }
    }
    return null
}

private fun NamedNodeMap.readInt(attributeName: String): Int? {
    return readString(attributeName)?.toIntOrNull()
}

private fun NamedNodeMap.readString(attributeName: String): String? {
    return getNamedItem(attributeName)?.nodeValue
}

private suspend fun checkpoint() {
    currentCoroutineContext().ensureActive()
}

private data class GifFrameDescriptor(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

private data class IntSize(
    val width: Int,
    val height: Int,
)

private data class IntRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

private enum class GifDisposalMethod {
    NONE,
    RESTORE_TO_BACKGROUND,
    RESTORE_TO_PREVIOUS,
}
