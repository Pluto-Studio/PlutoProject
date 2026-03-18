package plutoproject.feature.gallery.core.decode.decoder

import kotlinx.coroutines.*
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import plutoproject.feature.gallery.core.decode.*
import java.io.ByteArrayInputStream
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.metadata.IIOMetadata

internal data class GifFrameDescriptor(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

internal data class IntSize(
    val width: Int,
    val height: Int,
)

internal data class IntRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

internal enum class GifDisposalMethod {
    NONE,
    RESTORE_TO_BACKGROUND,
    RESTORE_TO_PREVIOUS,
}

private class DefaultGifDecoder(
    private val logger: Logger,
) : ImageDecoder {
    override suspend fun decode(bytes: ByteArray, constraints: DecodeConstraints): DecodeResult<DecodedImage> = try {
        decodeInternal(bytes = bytes, constraints = constraints)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.log(
            Level.WARNING,
            "Gif decode failed with private error: bytes=${bytes.size}, maxPixels=${constraints.maxPixels}, maxFrames=${constraints.maxFrames}",
            e,
        )
        DecodeResult.Failure(DecodeStatus.DECODE_FAILED)
    }

    private suspend fun decodeInternal(
        bytes: ByteArray,
        constraints: DecodeConstraints,
    ): DecodeResult<DecodedImage> = withContext(Dispatchers.IO) {
        val imageInput = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
            ?: return@withContext DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)

        imageInput.use { input ->
            val reader = ImageIO.getImageReadersByFormatName("gif").asSequence().firstOrNull()
                ?: return@withContext DecodeResult.Failure(DecodeStatus.DECODE_FAILED)
            try {
                reader.input = input

                val screenSize = readLogicalScreenSize(reader)
                val width = screenSize.width
                val height = screenSize.height
                if (width <= 0 || height <= 0) {
                    return@withContext DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
                }

                val framePixels = width.toLong() * height.toLong()
                if (framePixels > Int.MAX_VALUE.toLong()) {
                    return@withContext DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
                }
                if (framePixels > constraints.maxPixels.toLong()) {
                    return@withContext DecodeResult.Failure(DecodeStatus.IMAGE_TOO_LARGE)
                }

                val frameCount = reader.getNumImages(true)
                if (frameCount <= 0) {
                    return@withContext DecodeResult.Failure(DecodeStatus.INVALID_IMAGE)
                }
                if (frameCount > constraints.maxFrames) {
                    return@withContext DecodeResult.Failure(DecodeStatus.TOO_MANY_FRAMES)
                }

                val totalFramePixels = framePixels * frameCount.toLong()
                if (totalFramePixels > constraints.maxTotalFramePixels) {
                    return@withContext DecodeResult.Failure(DecodeStatus.IMAGE_TOO_LARGE)
                }

                val frameTimeline = scanFrameTimeline(reader, frameCount)

                DecodeResult.Success(
                    DecodedImage.Animated(
                        source = DefaultDecodedAnimatedImageSource(
                            width = width,
                            height = height,
                            frameCount = frameCount,
                            frameTimeline = frameTimeline,
                            opener = {
                                openGifFrameStream(
                                    bytes = bytes,
                                    width = width,
                                    height = height,
                                    frameTimeline = frameTimeline,
                                )
                            },
                        )
                    )
                )
            } finally {
                reader.dispose()
            }
        }
    }
}

fun defaultGifDecoder(logger: Logger): ImageDecoder = DefaultGifDecoder(logger = logger)

internal suspend fun checkpoint() {
    currentCoroutineContext().ensureActive()
}

internal suspend fun scanFrameTimeline(reader: ImageReader, frameCount: Int): List<AnimatedFrameTiming> {
    val timeline = ArrayList<AnimatedFrameTiming>(frameCount)
    for (index in 0 until frameCount) {
        checkpoint()
        val metadata = withContext(Dispatchers.IO) {
            reader.getImageMetadata(index)
        }
        val delayCentiseconds = readDelayCentiseconds(metadata)
        timeline += AnimatedFrameTiming(sourceFrameIndex = index, delayCentiseconds = delayCentiseconds)
    }
    return timeline
}

internal suspend fun openGifFrameStream(
    bytes: ByteArray,
    width: Int,
    height: Int,
    frameTimeline: List<AnimatedFrameTiming>,
): DecodedAnimatedFrameStream = withContext(Dispatchers.IO) {
    val imageInput = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        ?: throw IllegalArgumentException("failed to create image input stream")
    val reader = ImageIO.getImageReadersByFormatName("gif").asSequence().firstOrNull()
        ?: run {
            imageInput.close()
            throw IllegalArgumentException("failed to create gif reader")
        }

    try {
        reader.input = imageInput
    } catch (e: Exception) {
        reader.dispose()
        imageInput.close()
        throw e
    }

    DefaultDecodedAnimatedFrameStream(
        reader = reader,
        imageInput = imageInput,
        width = width,
        height = height,
        frameTimeline = frameTimeline,
    )
}

internal fun readLogicalScreenSize(reader: ImageReader): IntSize {
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

internal fun readFrameDescriptor(metadata: IIOMetadata): GifFrameDescriptor {
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

internal fun readDelayCentiseconds(metadata: IIOMetadata): Int {
    val root = runCatching { metadata.getAsTree("javax_imageio_gif_image_1.0") }.getOrNull() ?: return 0
    val graphicControl = root.findFirstNode("GraphicControlExtension") ?: return 0
    return graphicControl.attributes.readInt("delayTime")?.coerceAtLeast(0) ?: 0
}

internal fun readDisposalMethod(metadata: IIOMetadata): GifDisposalMethod {
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

internal fun resolvePatchRect(
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

internal fun drawPatch(
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

internal fun applyDisposal(
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
